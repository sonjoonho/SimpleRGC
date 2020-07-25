package simplergc.commands.batch.output

import java.io.File
import simplergc.commands.RGCTransduction
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CSVColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCSVColocalizationOutput(transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val csvColocalizationOutput = CSVColocalizationOutput(transductionParameters)

    override fun addTransductionResultForFile(transductionResult: RGCTransduction.TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        csvColocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    override fun output() {
        csvColocalizationOutput.createOutputFolder()
        csvColocalizationOutput.writeSummaryCsv()
        writeDocumentationCsv()
        for (metricName in getMetricMappings().keys) {
            writeMetricCsv(metricName)
        }
        csvColocalizationOutput.writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        for (row in documentationRows) {
            csvColocalizationOutput.documentationCsv.addRow(row)
        }
        csvColocalizationOutput.documentationCsv.produceCsv(File("${csvColocalizationOutput.outputPath}Documentation.csv"))
    }

    private fun writeMetricCsv(metricName: String) {
        val maxRows = getMaxRows()
        val metricData = getMetricData()
        for (rowIdx in 0..maxRows!!) {
            val rowData = getMetricMappings().getOrDefault(metricName, emptyList()).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(MetricRow(rowIdx, rowData))
        }
        metricData.produceCsv(File("${csvColocalizationOutput.outputPath}$metricName.csv"))
    }
}
