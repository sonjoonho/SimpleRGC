package simplergc.services.colocalizer.output

import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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
        // TODO: Left this code here in case anyone wants to pretty up the sheet
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

        // TODO: Modify following 2 sheets to work on a per file basis
        val summarySheet = workbook.createSheet("Summary")
        val summaryHeader = arrayOf(
            "File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "IntDen",
            "RawIntDen"
        )
        // Create header for summary
        val summaryHeaderRow = summarySheet.createRow(0)
        for (summaryCol in summaryHeader.indices) {
            val cell = summaryHeaderRow.createCell(summaryCol)
            cell.setCellValue(summaryHeader[summaryCol])
        }

        val perCellAnalysisSheet = workbook.createSheet("Transduced cells analysis")

        val paramsSheet = workbook.createSheet("Parameters")
        val paramsHeader = arrayOf(
            "File name",
            "Simple RGC plugin",
            "Version",
            "Pixel size (micrometers)",
            "Morphology channel",
            "Transduction channel",
            "Cell diameter (px)",
            "Local threshold radius",
            "Gaussian blur sigma",
            "Exclude axons"
        )
        // TODO: insert params here

        // Write file and close streams
        val fileOut = file.outputStream()
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
