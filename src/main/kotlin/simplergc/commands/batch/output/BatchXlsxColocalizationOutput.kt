package simplergc.commands.batch.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.commands.RGCTransduction
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.XlsxColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXlsxColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val xlsxColocalizationOutput = XlsxColocalizationOutput(transductionParameters)

    override fun addTransductionResultForFile(transductionResult: RGCTransduction.TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
        xlsxColocalizationOutput.addTransductionResultForFile(transductionResult, file)
    }

    override fun output() {
        val workbook = XSSFWorkbook()
        writeDocSheet(workbook)
        xlsxColocalizationOutput.writeSummarySheet(workbook)
        for (metricName in metricMappings().keys) {
            writeMetricSheet(metricName, workbook)
        }
        xlsxColocalizationOutput.writeParamsSheet(workbook)

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        print(outputXlsxFile.absoluteFile)
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        val docXlsx = Table(emptyList())
        for (row in documentationRows) {
            docXlsx.addRow(row)
        }
        docXlsx.produceXlsx(workbook, "Documentation")
    }

    private fun writeMetricSheet(metricName: String, workbook: XSSFWorkbook) {
        val maxRows = maxRows()
        val metricData = metricData()
        for (rowIdx in 0..maxRows) {
            val rowData = metricMappings().getValue(metricName).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(MetricRow(rowIdx, rowData))
        }
        metricData.produceXlsx(workbook, metricName)
    }
}
