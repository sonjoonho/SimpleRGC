package simplergc.commands.batch.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.XLSXColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXLSXColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val XLSXColocalizationOutput = XLSXColocalizationOutput(transductionParameters)

    override fun output() {
        val workbook = XSSFWorkbook()
        writeDocSheet(workbook)
        XLSXColocalizationOutput.writeSummarySheet(workbook)
        for (metricName in metricMappings.keys) {
            writeMetricSheet(metricName, workbook)
        }
        XLSXColocalizationOutput.writeParamsSheet(workbook)

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeMetricSheet(metricName: String, workbook: XSSFWorkbook) {
        TODO("Not yet implemented")
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        TODO("Not yet implemented")
    }
}
