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

enum class Metric(val value: String, val compute: (CellAnalysis) -> Int) {
    Area("Morphology Area", CellAnalysis::area),
    Mean("Mean Int", CellAnalysis::mean),
    Median("Median Int", CellAnalysis::median),
    Min("Min Int", CellAnalysis::min),
    Max("Max Int", CellAnalysis::max),
    IntDen("Raw IntDen", CellAnalysis::rawIntDen)
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
    abstract fun writeMetric(metric: Metric)

    fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        colocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    fun writeSheets() {
        writeDocumentation()
        colocalizationOutput.writeSummary()

        for (metric in Metric.values()) {
            writeMetric(metric)
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
    private fun metricMappings(): Map<Metric, MutableList<Pair<String, List<Int>>>> {
        return Metric.values().toList().associateWith { metric: Metric ->
            // Create [filenames and a list of values] to associate with metric
            val fileValues = mutableListOf<Pair<String, List<Int>>>()
            for ((fileName, result) in fileNameAndResultsList) {
                val cellValues = result.overlappingTransducedIntensityAnalysis.map { cell ->
                    metric.compute(cell)
                }
                fileValues.add(Pair(fileName, cellValues))
            }
            fileValues
        }
    }

    private fun maxRows(): Int {
        val results = fileNameAndResultsList.unzip().second
        val sizes = results.map { it.overlappingTransducedIntensityAnalysis.size }
        return sizes.max() ?: 0
    }

    fun metricData(metric: Metric): Table {
        val fileNames = fileNameAndResultsList.unzip().first
        val t = Table(listOf("Transduced Cell") + fileNames)
        val nRows = maxRows()
        val metricFileValues = metricMappings().getValue(metric)
        for (rowIdx in 0 until nRows) {
            val rowData = metricFileValues.map { it.second.getOrNull(rowIdx) }
            t.addRow(MetricRow(rowIdx + 1, rowData))
        }
        val fileValues = metricFileValues.unzip().second
        Aggregate.values().forEach { t.addRow(generateAggregateRow(it, fileValues)) }
        return t
    }
}
