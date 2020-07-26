package simplergc.services.batch.output

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

// Oooh what's that over there, look there instead.
typealias MetricMapping = Map<Metric, List<Pair<String, List<Pair<String, List<Int>>>>>>

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
    abstract fun writeMetric(metricMapping: MetricMapping, metric: Metric)

    fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        colocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    fun writeSheets() {
        writeDocumentation()
        colocalizationOutput.writeSummary()

        val metricMapping = computeMetricMapping()
        for (metric in Metric.values()) {
            writeMetric(metricMapping, metric)
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

    // Returns a map from metric to a list of [filenames and a list of values].
    private fun computeMetricMapping(): MetricMapping {
        // Check for no files before trying to get the number of channels
        if (fileNameAndResultsList.size == 0) {
            return Metric.values().toList().associateWith { listOf<Pair<String, List<Pair<String, List<Int>>>>>() }
        }
        // All transduction results will have the same channels
        val firstTransductionResult = fileNameAndResultsList[0].second
        val channelNames = firstTransductionResult.channelResults.map { it.name }

        val transductionChannel = colocalizationOutput.transductionParameters.transducedChannel

        // For each metric, generate a mapping to a list of [filenames and a list of values] pairs
        // Depending on the metric, we may wish to do this for all channels, or just the transduction channel
        return Metric.values().toList().associateWith { metric: Metric ->
            if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                listOf(Pair(metric.value, computeMetricValuesForChannel(metric, transductionChannel)))
            } else {
                // Compute the metric values for all channels
                val channelFileValues = mutableListOf<Pair<String, List<Pair<String, List<Int>>>>>()
                for (channel in channelNames.indices) {
                    channelFileValues.add(Pair("${metric.value}-${channelNames[channel]}", computeMetricValuesForChannel(metric, channel)))
                }
                channelFileValues
            }
        }
    }

    private fun computeMetricValuesForChannel(metric: Metric, channelIdx: Int): List<Pair<String, List<Int>>> {
        val fileValues = mutableListOf<Pair<String, List<Int>>>()
        for ((fileName, result) in fileNameAndResultsList) {
            val cellValues = result.channelResults[channelIdx].cellAnalyses.map { cell ->
                metric.compute(cell)
            }
            fileValues.add(Pair(fileName, cellValues))
        }
        return fileValues
    }

    private fun maxRows(): Int {
        // All channels will have the same number of cells, so just use transduced channel
        val transducedChannel = colocalizationOutput.transductionParameters.transducedChannel

        val results = fileNameAndResultsList.unzip().second
        val sizes = results.map { it.channelResults[transducedChannel].cellAnalyses.size }
        return sizes.max() ?: 0
    }

    fun metricData(metricMapping: MetricMapping, metric: Metric): List<Pair<String, Table>> {
        val (fileNames, results) = fileNameAndResultsList.unzip()
        val channelTables = mutableListOf<Pair<String, Table>>()

        val nRows = maxRows()
        val metricChannelValues = metricMapping.getValue(metric)
        for (metricChannel in metricChannelValues) {
            val t = Table(listOf("Transduced Cell") + fileNames)
            for (rowIdx in 0 until nRows) {
                val rowData = metricChannel.second.map { it.second.getOrNull(rowIdx) }
                t.addRow(MetricRow(rowIdx + 1, rowData))
            }
            val fileValues = metricChannel.second.unzip().second
            Aggregate.values().forEach { t.addRow(generateAggregateRow(it, fileValues)) }
            channelTables.add(Pair(metricChannel.first, t))
        }
        return channelTables
    }
}
