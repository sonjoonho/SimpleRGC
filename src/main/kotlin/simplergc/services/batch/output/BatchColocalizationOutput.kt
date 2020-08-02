package simplergc.services.batch.output

import kotlin.math.max
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.Metric
import simplergc.services.MetricRow
import simplergc.services.Output
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput
import simplergc.services.colocalizer.output.DocumentationRow

abstract class BatchColocalizationOutput : Output {

    private val fileNameAndResultsList = mutableListOf<Pair<String, TransductionResult>>()

    abstract val colocalizationOutput: ColocalizationOutput
    abstract fun generateAggregateRow(aggregate: Aggregate, rawValues: List<List<Int>>, spaces: Int = 0): AggregateRow
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

    fun documentationData(): Table {
        val channelNames = colocalizationOutput.channelNames()
        return Table().apply {
            addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
            addRow(DocumentationRow("", ""))
            addRow(DocumentationRow("Abbreviation", "Description"))
            addRow(DocumentationRow("Summary", "Key measurements per image"))
            Metric.values().forEach { metric ->
                if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                    addRow(DocumentationRow(metric.value, metric.description))
                } else {
                    for (name in channelNames) {
                        addRow(DocumentationRow("${metric.value}_$name", "${metric.description} for $name"))
                    }
                }
            }
            addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
        }
    }

    private fun computeMetricTables(): List<Pair<String, Table>> {
        // Check for no files before trying to get the number of channels
        if (fileNameAndResultsList.size == 0) {
            return emptyList()
        }

        val channelNames = colocalizationOutput.channelNames()
        val transductionChannel = colocalizationOutput.transductionParameters.transducedChannel

        return Metric.values().map { metric: Metric ->
            if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                listOf(
                    Pair(metric.value, computeMetricTableForChannel(metric, transductionChannel))
                )
            } else {
                // Compute the metric values for all channels
                channelNames.mapIndexed { idx, name ->
                    Pair("${metric.value}_$name", computeMetricTableForChannel(metric, idx))
                }
            }
        }.flatten()
    }

    private fun computeMetricTableForChannel(metric: Metric, channelIdx: Int): Table {
        val headers = mutableListOf(HeaderField("Transduced Cell"))
        var maxRows = 0

        // Generate all of the values for each file
        val rawValues = mutableListOf<List<Int>>()
        val fileValues = mutableListOf<Pair<String, List<Int>>>()

        for ((fileName, result) in fileNameAndResultsList) {
            val cellValues = result.channelResults[channelIdx].cellAnalyses.map { cell ->
                metric.compute(cell)
            }
            fileValues.add(Pair(fileName, cellValues))
            rawValues.add(cellValues)
            headers.add(HeaderField(fileName))
            maxRows = max(maxRows, cellValues.size)
        }

        val t = Table()
        t.addRow(FieldRow(headers))
        // Populate each row since we have the values for each file
        for (rowIdx in 0 until maxRows) {
            val rowData = fileValues.map { it.second.getOrNull(rowIdx) }
            t.addRow(MetricRow(rowIdx + 1, rowData))
        }

        Aggregate.values().forEach { t.addRow(generateAggregateRow(it, rawValues, 0)) }
        return t
    }
}
