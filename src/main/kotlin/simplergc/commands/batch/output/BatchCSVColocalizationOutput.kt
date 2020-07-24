package simplergc.commands.batch.output

import simplergc.commands.RGCTransduction
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CSVColocalizationOutput
import java.io.File

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
        csvColocalizationOutput.checkOutputFolderCanBeCreated()
        csvColocalizationOutput.writeSummaryCsv()
        writeDocumentationCsv()
        for (metricName in getMetricMappings().keys) {
            writeMetricCSV(metricName)
        }
        csvColocalizationOutput.writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        for (row in documentationRows) {
            csvColocalizationOutput.documentationCsv.addRow(row)
        }
        csvColocalizationOutput.documentationCsv.produceCSV(File("${csvColocalizationOutput.outputPath}Documentation.csv"))
    }

    private fun writeMetricCSV(metricName: String) {
        val maxRows = getMaxRows()
        val metricData = getMetricData()
        for (rowIdx in 0..maxRows!!) {
            val rowData = getMetricMappings().getOrDefault(metricName, emptyList()).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(metricRow(rowIdx, rowData))
        }
        metricData.produceCSV(File("${csvColocalizationOutput.outputPath}${metricName}.csv"))
    }
}
