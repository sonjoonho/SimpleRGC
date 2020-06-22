package simplergc.services.colocalizer.output

import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XLSXColocalizationOutput(
    private val result: TransductionResult,
    private val file: File
) : SimpleOutput() {

    override fun output() {

        // Create Excel File.
        val workbook = XSSFWorkbook()
        val createHelper = workbook.creationHelper

        // Set header font to blue and bold.
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.BLUE.getIndex()

        val headerCellStyle = workbook.createCellStyle()
        headerCellStyle.setFont(headerFont)

        // Create sheets
        val docSheet = workbook.createSheet("Documentation")
        val summarySheet = workbook.createSheet("Summary")
        val perCellAnalysisSheet = workbook.createSheet("Per-cell analysis")
        val paramsSheet = workbook.createSheet("Parameters")

        // Write file and close streams
        val fileOut = file.outputStream()
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
