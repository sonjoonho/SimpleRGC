package simplergc.services.counter.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import java.io.File
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.CellDiameterRange

class XLSXCounterOutput(
    private val outputFile: File,
    private val morphologyChannel: Int,
    private val cellDiameterRange: CellDiameterRange,
    private val localThresholdRadius: Int,
    private val gaussianBlurSigma: Double
) : CounterOutput() {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    private val resultsFields = arrayOf(
        "File Name",
        "Cell Count"
    )

    private val parametersFields = arrayOf(
        "File Name",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    )

    /** Add cell count for a filename. */
    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    /**
     * Generate a citation and add to the [sheet] at the [rowNumber].
     */
    private fun generateCitation(sheet: XSSFSheet, rowNumber: Int) {
        val articleRow = sheet.createRow(rowNumber)
        articleRow.createCell(0).setCellValue("The article:")
        articleRow.createCell(1).setCellValue(ARTICLE_CITATION)
    }

    /**
     * Generate a table header and add to the [sheet] at the [rowNumber].
     */
    private fun generateTableHeader(
        sheet: XSSFSheet,
        headers: Array<String>,
        rowNumber: Int,
        headerCellStyle: XSSFCellStyle?
    ) {
        val headerRow = sheet.createRow(rowNumber)
        for (col in headers.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(headers[col])
            cell.cellStyle = headerCellStyle
        }
    }

    private fun createHeaderCellStyle(workbook: XSSFWorkbook): XSSFCellStyle? {
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.BLUE.getIndex()

        val headerCellStyle = workbook.createCellStyle()
        headerCellStyle.setFont(headerFont)
        return headerCellStyle
    }

    /**
     * Generate the 'Results' sheet, containing the cell counts for each filename.
     */
    private fun generateResultsSheet(workbook: XSSFWorkbook) {
        val resultsSheet = workbook.createSheet("Results")

        val createHelper = workbook.creationHelper

        val headerCellStyle = createHeaderCellStyle(workbook)

        generateCitation(resultsSheet, 0)
        generateTableHeader(resultsSheet, resultsFields, 2, headerCellStyle)

        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")

        var rowIdx = 3
        for (pair in fileNameAndCountList) {
            val row = resultsSheet.createRow(rowIdx++)

            // File Name
            row.createCell(0).setCellValue(pair.first.replace(",", ""))

            // Cell Count
            val countCell = row.createCell(1)
            countCell.setCellValue(pair.second.toDouble())
        }

        for (i in 0..resultsFields.size) {
            resultsSheet.autoSizeColumn(i)
        }
    }

    /**
     * Generate the 'Parameters' sheet, containing the parameters used for each filename.
     */
    private fun generateParametersSheet(workbook: XSSFWorkbook) {
        val parametersSheet = workbook.createSheet("Parameters")

        val headerCellStyle = createHeaderCellStyle(workbook)

        generateTableHeader(parametersSheet, parametersFields, 0, headerCellStyle)

        var rowIdx = 1
        for (pair in fileNameAndCountList) {
            val row = parametersSheet.createRow(rowIdx++)
            var colIdx = 0
            // File Name
            row.createCell(colIdx++).setCellValue(pair.first.replace(",", ""))

            // Plugin Information
            val pluginNameCell = row.createCell(colIdx++)
            pluginNameCell.setCellValue(PLUGIN_NAME)

            val pluginVersionCell = row.createCell(colIdx++)
            pluginVersionCell.setCellValue(PLUGIN_VERSION)

            // Parameters
            val morphologyChannelCell = row.createCell(colIdx++)
            morphologyChannelCell.setCellValue(morphologyChannel.toDouble())

            val smallestDiameterCell = row.createCell(colIdx++)
            smallestDiameterCell.setCellValue(cellDiameterRange.smallest)

            val largestDiameterCell = row.createCell(colIdx++)
            largestDiameterCell.setCellValue(cellDiameterRange.largest)

            val localThresholdRadiusCell = row.createCell(colIdx++)
            localThresholdRadiusCell.setCellValue(localThresholdRadius.toDouble())

            val gaussianBlurSigmaCell = row.createCell(colIdx)
            gaussianBlurSigmaCell.setCellValue(gaussianBlurSigma)
        }

        for (i in 0..parametersFields.size) {
            parametersSheet.autoSizeColumn(i)
        }
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        generateResultsSheet(workbook)
        generateParametersSheet(workbook)

        val outputXlsxFile = File(FilenameUtils.removeExtension(outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }
}
