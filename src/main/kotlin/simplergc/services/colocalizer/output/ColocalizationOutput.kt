package simplergc.services.colocalizer.output

import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.BaseRow
import simplergc.services.BooleanField
import simplergc.services.CellColocalizationService
import simplergc.services.DoubleField
import simplergc.services.EmptyRow
import simplergc.services.Field
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.IntField
import simplergc.services.Metric
import simplergc.services.Metric.ChannelSelection.TRANSDUCTION_ONLY
import simplergc.services.Output
import simplergc.services.Output.Companion.ARTICLE_CITATION
import simplergc.services.Parameters
import simplergc.services.StringField
import simplergc.services.Table

data class DocumentationRow(val key: String, val description: String) : BaseRow {
    override fun toList() = listOf(StringField(key), StringField(description))
}

data class ParametersRow(
    val fileName: String,
    val morphologyChannel: Int,
    val excludeAxonsFromMorphologyChannel: Boolean,
    val transductionChannel: Int,
    val excludeAxonsFromTransductionChannel: Boolean,
    val smallestCellDiameter: Double,
    val largestCellDiameter: Double,
    val localThresholdRadius: Int,
    val gaussianBlurSigma: Double
) : BaseRow {
    override fun toList() = listOf(
        StringField(fileName),
        StringField(ColocalizationOutput.PLUGIN_NAME),
        StringField(Output.PLUGIN_VERSION),
        IntField(morphologyChannel),
        BooleanField(excludeAxonsFromMorphologyChannel),
        IntField(transductionChannel),
        BooleanField(excludeAxonsFromTransductionChannel),
        DoubleField(smallestCellDiameter),
        DoubleField(largestCellDiameter),
        IntField(localThresholdRadius),
        DoubleField(gaussianBlurSigma)
    )
}

data class SummaryRow(
    val fileName: String,
    val summary: TransductionResult
) : BaseRow {
    override fun toList(): List<Field<*>> {
        val fields = mutableListOf(
            StringField(fileName),
            IntField(summary.targetCellCount),
            IntField(summary.transducedCellCount),
            DoubleField(summary.transductionEfficiency),
            IntField(summary.channelResults[0].avgMorphologyArea)
        )
        // Add each channel's metric values grouped by metric
        summary.channelResults.forEach { channelResult ->
            fields.add(DoubleField(channelResult.meanFluorescenceIntensity))
        }
        summary.channelResults.forEach { channelResult ->
            fields.add(IntField(channelResult.medianFluorescenceIntensity))
        }
        summary.channelResults.forEach { channelResult ->
            fields.add(IntField(channelResult.minFluorescenceIntensity))
        }
        summary.channelResults.forEach { channelResult ->
            fields.add(IntField(channelResult.maxFluorescenceIntensity))
        }
        summary.channelResults.forEach { channelResult ->
            fields.add(IntField(channelResult.rawIntDen))
        }
        return fields
    }
}

data class SingleChannelTransductionAnalysisRow(
    val fileName: String,
    val transducedCell: Int,
    val cellAnalysis: CellColocalizationService.CellAnalysis
) : BaseRow {
    override fun toList(): List<Field<*>> {
        val fields = mutableListOf(
            StringField(fileName),
            IntField(transducedCell)
        )
        for (metric in Metric.values()) {
            fields.add(IntField(metric.compute(cellAnalysis).toInt()))
        }
        return fields
    }
}

data class MultiChannelTransductionAnalysisRow(
    val fileName: String,
    val transducedCell: Int,
    val cellAnalyses: List<CellColocalizationService.CellAnalysis>,
    val transductionChannel: Int
) : BaseRow {
    override fun toList(): List<Field<*>> {
        val fields = mutableListOf(
            StringField(fileName),
            IntField(transducedCell)
        )
        for (metric in Metric.values()) {
            if (metric.channels == TRANSDUCTION_ONLY) {
                fields.add(IntField(metric.compute(cellAnalyses[transductionChannel]).toInt()))
            } else {
                for (cellAnalysis in cellAnalyses) {
                    fields.add(IntField(metric.compute(cellAnalysis).toInt()))
                }
            }
        }
        return fields
    }
}

/**
 * Outputs the result of cell counting.
 */
abstract class ColocalizationOutput(val transductionParameters: Parameters.Transduction) : Output {

    val fileNameAndResultsList = mutableListOf<Pair<String, TransductionResult>>()

    val transducedChannel = transductionParameters.transducedChannel

    companion object {
        const val PLUGIN_NAME = "RGC Transduction"
    }

    open fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
    }

    abstract fun getSummaryTable(): Table
    abstract fun writeSummaryWithAggregates()
    abstract fun writeSummary()
    abstract fun writeAnalysis()
    abstract fun writeParameters()
    abstract fun writeDocumentation()

    // spaces is used for csv aggregate generator (to avoid formatting issues.)
    // startRow is only used by xlsx aggregate generator
    abstract fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Number>>,
        spaces: Int,
        startRow: Int = 2
    ): AggregateRow

    fun addTotalRow(t: Table, rawCellCounts: List<Int>, rawTransducedCellCounts: List<Int>): Table {
        val totalRow = AggregateRow(
            "Total",
            listOf(IntField(rawCellCounts.sum()), IntField(rawTransducedCellCounts.sum())),
            spaces = 0
        )
        t.addRow(totalRow)
        return t
    }

    // NOTE: This may be a very inefficient function, but there is no easier way to loop over columns of data.
    fun getSummaryRawValues(): MutableList<List<Number>> {
        val rawValues = mutableListOf<List<Number>>()
        val rawCellCounts = mutableListOf<Int>()
        val rawTransducedCellCounts = mutableListOf<Int>()
        val rawTransductionEfficiencies = mutableListOf<Number>()
        val rawMorphAreas = mutableListOf<Number>()
        val numChannels = channelNames().size
        val rawChannelMeans = Array<MutableList<Number>>(numChannels) { mutableListOf() }
        val rawChannelMedians = Array<MutableList<Number>>(numChannels) { mutableListOf() }
        val rawChannelMins = Array<MutableList<Number>>(numChannels) { mutableListOf() }
        val rawChannelMaxs = Array<MutableList<Number>>(numChannels) { mutableListOf() }
        val rawChannelIntDens = Array<MutableList<Number>>(numChannels) { mutableListOf() }
        for ((_, result) in fileNameAndResultsList) {
            rawCellCounts.add(result.targetCellCount)
            rawTransducedCellCounts.add(result.transducedCellCount)
            rawTransductionEfficiencies.add(result.transductionEfficiency)
            rawMorphAreas.add(result.channelResults[0].avgMorphologyArea)
            for (i in 0 until numChannels) {
                rawChannelMeans[i].add(result.channelResults[i].meanFluorescenceIntensity)
                rawChannelMedians[i].add(result.channelResults[i].medianFluorescenceIntensity)
                rawChannelMins[i].add(result.channelResults[i].minFluorescenceIntensity)
                rawChannelMaxs[i].add(result.channelResults[i].maxFluorescenceIntensity)
                rawChannelIntDens[i].add(result.channelResults[i].rawIntDen)
            }
        }
        rawValues.addAll(listOf(rawCellCounts, rawTransducedCellCounts, rawTransductionEfficiencies, rawMorphAreas))
        rawValues.addAll(rawChannelMeans)
        rawValues.addAll(rawChannelMedians)
        rawValues.addAll(rawChannelMins)
        rawValues.addAll(rawChannelMaxs)
        rawValues.addAll(rawChannelIntDens)
        return rawValues
    }

    fun channelNames(): List<String> {
        if (fileNameAndResultsList.size == 0) {
            return emptyList()
        }
        // All transduction results will have the same channels
        val firstTransductionResult = fileNameAndResultsList[0].second
        return firstTransductionResult.channelResults.map { it.name }
    }

    fun documentationData(): Table = Table().apply {
        addRow(DocumentationRow("The article: ", ARTICLE_CITATION))
        addRow(EmptyRow())
        addRow(FieldRow(listOf(HeaderField("Abbreviation"), HeaderField("Description"))))
        addRow(DocumentationRow("Summary", "Key measurements per image"))
        addRow(DocumentationRow("Transduced cells analysis", "Per-cell metrics of transduced cells"))
        addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
    }

    fun parameterData(): Table {
        val t = Table()
        t.addRow(FieldRow(listOf(
            "File Name",
            "SimpleRGC Plugin",
            "Plugin Version",
            "Morphology Channel",
            "Exclude Axons from Morphology Channel?",
            "Transduction Channel",
            "Exclude Axons from Transduction Channel?",
            "Smallest Cell Diameter (px)",
            "Largest Cell Diameter (px)",
            "Local Threshold Radius",
            "Gaussian Blur Sigma"
        ).map { HeaderField(it) }))
        // Add parameter data.
        for ((fileName, _) in fileNameAndResultsList) {
            t.addRow(
                ParametersRow(
                    fileName = fileName,
                    morphologyChannel = transductionParameters.targetChannel,
                    excludeAxonsFromMorphologyChannel = transductionParameters.shouldRemoveAxonsFromTargetChannel,
                    transductionChannel = transductionParameters.transducedChannel,
                    excludeAxonsFromTransductionChannel = transductionParameters.shouldRemoveAxonsFromTransductionChannel,
                    smallestCellDiameter = transductionParameters.cellDiameterRange.smallest,
                    largestCellDiameter = transductionParameters.cellDiameterRange.largest,
                    localThresholdRadius = transductionParameters.localThresholdRadius,
                    gaussianBlurSigma = transductionParameters.gaussianBlurSigma
                )
            )
        }
        return t
    }
}
