package simplergc.commands.batch.output

import simplergc.commands.RGCTransduction
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CsvColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCsvColocalizationOutput(transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val csvColocalizationOutput = CsvColocalizationOutput(transductionParameters)

    override fun addTransductionResultForFile(transductionResult: RGCTransduction.TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        csvColocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    override fun output() {
        csvColocalizationOutput.createOutputFolder()
        csvColocalizationOutput.writeSummaryCsv()
        writeDocumentationCsv()
        for (metric in Metrics.values()) {
            writeMetricCsv(metric)
        }
        csvColocalizationOutput.writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        for (row in documentationRows) {
            csvColocalizationOutput.documentationCsv.addRow(row)
        }
        csvColocalizationOutput.documentationCsv.produceCsv(File("${csvColocalizationOutput.outputPath}Documentation.csv"))
    }

    private fun writeMetricCsv(metric: Metrics) {
        val maxRows = maxRows()
        val metricData = metricData()
        for (rowIdx in 0..maxRows) {
            val rowData = metricMappings().getValue(metric).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(MetricRow(rowIdx, rowData))
        }
        metricData.produceCsv(File("${csvColocalizationOutput.outputPath}${metric.value}.csv"))
    }
}
