package simplergc.services.colocalizer.output

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
        // val headerFont = workbook.createFont()
        // headerFont.bold = true
        // headerFont.color = IndexedColors.BLUE.getIndex()
        //
        // val headerCellStyle = workbook.createCellStyle()
        // headerCellStyle.setFont(headerFont)

        // Create sheets
        val docSheet = workbook.createSheet("Documentation")
        val docInfo = ArrayList<Array<String>>(
            arrayListOf(
                arrayOf("The article: ", "TODO: Insert citation"),
                arrayOf("", ""),
                arrayOf("Abbreviation", "Description"),
                arrayOf("Summary", "Key measurements per image"),
                arrayOf("Transduced cells analysis", "Per-cell metrics of transduced cells"),
                arrayOf("Parameters", "Parameters used to run the SimpleRGC plugin")
            )
        )
        docInfo.forEachIndexed { rowIdx, rows ->
            run {
                val row = docSheet.createRow(rowIdx)
                rows.forEachIndexed { colIdx, str ->
                    run {
                        val cell = row.createCell(colIdx)
                        cell.setCellValue(str)
                    }
                }
            }
        }

        val summarySheet = workbook.createSheet("Summary")
        val perCellAnalysisSheet = workbook.createSheet("Transduced cells analysis")
        val paramsSheet = workbook.createSheet("Parameters")

        // Write file and close streams
        val fileOut = file.outputStream()
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
