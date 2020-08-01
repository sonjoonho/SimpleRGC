package simplergc.services.colocalizer.output

import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.BaseRow
import simplergc.services.BooleanField
import simplergc.services.CellColocalizationService
import simplergc.services.DoubleField
import simplergc.services.Field
import simplergc.services.HeaderRow
import simplergc.services.IntField
import simplergc.services.Output
import simplergc.services.Parameters
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.batch.output.Metric

private const val UTF_8_SUP2 = "\u00b2"

data class DocumentationRow(val key: String, val description: String) : BaseRow {
    override fun toList() = listOf(StringField(key), StringField(description))
}

data class ParametersRow(
    val fileName: String,
    val morphologyChannel: Int,
    val excludeAxonsFromMorphologyChannel: Boolean,
    val transductionChannel: Int,
    val excludeAxonsFromTransductionChannel: Boolean,
    val cellDiameterText: String,
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
        StringField(cellDiameterText),
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
            fields.add(IntField(channelResult.meanFluorescenceIntensity))
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

data class TransductionAnalysisRow(
    val fileName: String,
    val transducedCell: Int,
    val cellAnalysis: CellColocalizationService.CellAnalysis
) : BaseRow {
    override fun toList() = listOf(
        StringField(fileName),
        IntField(transducedCell),
        IntField(cellAnalysis.area),
        IntField(cellAnalysis.mean),
        IntField(cellAnalysis.median),
        IntField(cellAnalysis.min),
        IntField(cellAnalysis.max),
        IntField(cellAnalysis.rawIntDen)
    )
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

    abstract fun writeSummary()
    abstract fun writeAnalysis()
    abstract fun writeParameters()
    abstract fun writeDocumentation()

    fun channelNames(): List<String> {
        if (fileNameAndResultsList.size == 0) {
            return emptyList()
        }
        // All transduction results will have the same channels
        val firstTransductionResult = fileNameAndResultsList[0].second
        return firstTransductionResult.channelResults.map { it.name }
    }

    fun documentationData(): Table = Table().apply {
        addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
        addRow(DocumentationRow("", ""))
        addRow(DocumentationRow("Abbreviation", "Description"))
        addRow(DocumentationRow("Summary", "Key measurements per image"))
        addRow(DocumentationRow("Transduced cells analysis", "Per-cell metrics of transduced cells"))
        addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
    }

    fun summaryData(): Table {
        val channelNames = channelNames()
        val headers = mutableListOf("File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel$UTF_8_SUP2)"
        )

        val metricColumns = listOf("Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluominrescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen")

        for (metricColumn in metricColumns) {
            for (channelName in channelNames) {
                headers.add("$metricColumn - $channelName")
            }
        }
        val t = Table()

        t.addRow(HeaderRow(headers))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        return t
    }

    fun analysisData(channelIdx: Int): Table {
        val t = Table()
        t.addRow(HeaderRow(listOf(
            "File Name",
            "Transduced Cell",
            "Morphology Area (pixel$UTF_8_SUP2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen"
        )))
        for ((fileName, result) in fileNameAndResultsList) {
            result.channelResults[channelIdx].cellAnalyses.forEachIndexed { i, cellAnalysis ->
                t.addRow(
                    TransductionAnalysisRow(
                        fileName = fileName,
                        transducedCell = i + 1,
                        cellAnalysis = cellAnalysis
                    )
                )
            }
            Aggregate.values().forEach {
                val rawValues = mutableListOf<List<Int>>()
                Metric.values().forEach { metric ->
                    rawValues.add(result.channelResults[channelIdx].cellAnalyses.map { cell ->
                        metric.compute(cell)
                    })
                }
                t.addRow(generateAggregateRow(it, rawValues, spaces = 1))
            }
        }
        return t
    }

    abstract fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Int>>,
        spaces: Int
    ): AggregateRow

    fun parameterData(): Table {
        val t = Table()
        t.addRow(HeaderRow(listOf(
            "File Name",
            "SimpleRGC Plugin",
            "Plugin Version",
            "Morphology channel",
            "Exclude Axons from morphology channel?",
            "Transduction channel",
            "Exclude Axons from transduction channel?",
            "Cell diameter range (px)",
            "Local threshold radius",
            "Gaussian blur sigma"
        )))
        // Add parameter data.
        for ((fileName, _) in fileNameAndResultsList) {
            t.addRow(
                ParametersRow(
                    fileName = fileName,
                    morphologyChannel = transductionParameters.targetChannel,
                    excludeAxonsFromMorphologyChannel = transductionParameters.shouldRemoveAxonsFromTargetChannel,
                    transductionChannel = transductionParameters.transducedChannel,
                    excludeAxonsFromTransductionChannel = transductionParameters.shouldRemoveAxonsFromTransductionChannel,
                    cellDiameterText = transductionParameters.cellDiameterText,
                    localThresholdRadius = transductionParameters.localThresholdRadius,
                    gaussianBlurSigma = transductionParameters.gaussianBlurSigma
                )
            )
        }
        return t
    }
}
