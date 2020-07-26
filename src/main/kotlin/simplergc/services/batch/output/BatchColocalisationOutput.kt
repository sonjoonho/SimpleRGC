package simplergc.services.batch.output

import kotlin.math.max
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.AggregateRow
import simplergc.services.CellColocalizationService.CellAnalysis
import simplergc.services.Field
import simplergc.services.MetricRow
import simplergc.services.Output
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput
import simplergc.services.colocalizer.output.DocumentationRow

enum class Metric(val value: String, val compute: (CellAnalysis) -> Int, val channels: ChannelSelection) {
    Area("Morphology Area", CellAnalysis::area, ChannelSelection.TRANSDUCTION_ONLY),
    Mean("Mean Int", CellAnalysis::mean, ChannelSelection.ALL_CHANNELS),
    Median("Median Int", CellAnalysis::median, ChannelSelection.ALL_CHANNELS),
    Min("Min Int", CellAnalysis::min, ChannelSelection.ALL_CHANNELS),
    Max("Max Int", CellAnalysis::max, ChannelSelection.ALL_CHANNELS),
    IntDen("Raw IntDen", CellAnalysis::rawIntDen, ChannelSelection.ALL_CHANNELS);

    enum class ChannelSelection {
        TRANSDUCTION_ONLY,
        ALL_CHANNELS
    }
}

enum class Aggregate(val abbreviation: String, val generateValue: (AggregateGenerator) -> Field<*>) {
    Mean("Mean", AggregateGenerator::generateMean),
    StandardDeviation("Std Dev", AggregateGenerator::generateStandardDeviation),
    StandardErrorOfMean("SEM", AggregateGenerator::generateStandardErrorOfMean),
    Count("N", AggregateGenerator::generateCount)
}

abstract class AggregateGenerator {
    abstract fun generateMean(): Field<*>
    abstract fun generateStandardDeviation(): Field<*>
    abstract fun generateStandardErrorOfMean(): Field<*>
    abstract fun generateCount(): Field<*>
}

abstract class BatchColocalizationOutput : Output {

    private val fileNameAndResultsList = mutableListOf<Pair<String, TransductionResult>>()

    abstract val colocalizationOutput: ColocalizationOutput
    abstract fun generateAggregateRow(aggregate: Aggregate, fileValues: List<List<Int>>): AggregateRow
    abstract fun writeDocumentation()
    abstract fun writeMetric(name: String, table: Table)

    fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        colocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    fun writeSheets() {
        writeDocumentation()
        colocalizationOutput.writeSummary()

        for (metricTable in computeMetricTables()) {
            writeMetric(metricTable.first, metricTable.second)
        }

        colocalizationOutput.writeParameters()
    }

    fun documentationData(): Table = Table(listOf()).apply {
        addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
        addRow(DocumentationRow("", ""))
        addRow(DocumentationRow("Abbreviation", "Description"))
        addRow(DocumentationRow("Summary", "Key measurements per image"))
        addRow(DocumentationRow("Mean Int: ", "Mean fluorescence intensity for each transduced cell"))
        addRow(DocumentationRow("Median Int:", "Median fluorescence intensity for each transduced cell"))
        addRow(DocumentationRow("Min Int: ", "Min fluorescence intensity for each transduced cell"))
        addRow(DocumentationRow("Max Int: ", "Max fluorescence intensity for each transduced cell"))
        addRow(DocumentationRow("Raw IntDen:", "Raw Integrated Density for each transduced cell"))
        addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
    }

    private fun computeMetricTables(): List<Pair<String, Table>> {
        // Check for no files before trying to get the number of channels
        if (fileNameAndResultsList.size == 0) {
            return emptyList()
        }
        // All transduction results will have the same channels
        val firstTransductionResult = fileNameAndResultsList[0].second
        val channelNames = firstTransductionResult.channelResults.map { it.name }

        val transductionChannel = colocalizationOutput.transductionParameters.transducedChannel

        return Metric.values().map { metric: Metric ->
            if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                listOf(
                    Pair(metric.value, computeMetricTableForChannel(metric, transductionChannel))
                )
            } else {
                // Compute the metric values for all channels
                channelNames.mapIndexed { idx, name ->
                    Pair("${metric.value}-$name", computeMetricTableForChannel(metric, idx))
                }
            }
        }.flatten()
    }

    private fun computeMetricTableForChannel(metric: Metric, channelIdx: Int): Table {
        val schema = mutableListOf("Transduced Cell")
        var maxRows = 0

        // Generate all of the values for each file
        val fileValues = mutableListOf<Pair<String, List<Int>>>()
        for ((fileName, result) in fileNameAndResultsList) {
            val cellValues = result.channelResults[channelIdx].cellAnalyses.map { cell ->
                metric.compute(cell)
            }
            fileValues.add(Pair(fileName, cellValues))

            schema.add(fileName)
            maxRows = max(maxRows, cellValues.size)
        }

        val t = Table(schema)
        // Populate each row since we have the values for each file
        for (rowIdx in 0 until maxRows) {
            val rowData = fileValues.map { it.second.getOrNull(rowIdx) }
            t.addRow(MetricRow(rowIdx + 1, rowData))
        }

        val rawValues = fileValues.unzip().second
        Aggregate.values().forEach { t.addRow(generateAggregateRow(it, rawValues)) }
        return t
    }
}
