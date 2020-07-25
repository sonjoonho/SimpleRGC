package simplergc.commands.batch.output

import java.io.File
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CsvColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCsvColocalizationOutput(transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput(CsvColocalizationOutput(transductionParameters)) {

    private val csvColocalizationOutput = CsvColocalizationOutput(transductionParameters)

    override fun output() {
        csvColocalizationOutput.createOutputFolder()
        csvColocalizationOutput.writeSummary()
        writeDocumentationCsv()
        for (metric in Metric.values()) {
            writeMetricSheet(metric)
        }
        csvColocalizationOutput.writeParameters()
    }

    private fun writeDocumentationCsv() {
        for (row in documentationRows) {
            csvColocalizationOutput.documentationCsv.addRow(row)
        }
        csvColocalizationOutput.documentationCsv.produceCsv(File("${csvColocalizationOutput.outputPath}Documentation.csv"))
    }

    override fun writeMetricSheet(metric: Metric) {
        val maxRows = maxRows()
        val metricData = metricData()
        for (rowIdx in 0..maxRows) {
            val rowData = metricMappings().getValue(metric).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(MetricRow(rowIdx, rowData))
        }
        metricData.produceCsv(File("${csvColocalizationOutput.outputPath}${metric.value}.csv"))
    }
}
