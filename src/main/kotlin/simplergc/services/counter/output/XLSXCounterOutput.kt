package simplergc.services.counter.output

import org.apache.poi.xssf.usermodel.XSSFSheet
import java.io.File
import java.io.FileOutputStream
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

    private val resultFields = arrayOf(
        "File Name",
        "Cell Count"
    )

    private val parameterFields = arrayOf(
        "File Name",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    )

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    private fun insertCitation(sheet: XSSFSheet, rowNumber: Int){
        val articleRow = sheet.createRow(rowNumber)
        articleRow.createCell(0).setCellValue("The article:")
        articleRow.createCell(1).setCellValue("[insert full citation]")
    }

    private fun insertTableHeader(sheet: XSSFSheet, headers: Array<String>, rowNumber: Int){
        val headerRow = sheet.createRow(rowNumber)
        for (col in headers.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(headers[col])
        }
    }

    private fun insertResultsSheet(workbook: XSSFWorkbook) {
        val resultsSheet = workbook.createSheet("Results")

        val createHelper = workbook.creationHelper

        insertCitation(resultsSheet, 0)
        insertTableHeader(resultsSheet, resultFields, 2)

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

        for (i in 0..resultFields.size) {
            resultsSheet.autoSizeColumn(i)
        }
    }

    private fun insertParametersSheet(workbook: XSSFWorkbook) {
        val parametersSheet = workbook.createSheet("Parameters")

        insertTableHeader(parametersSheet, parameterFields, 0)

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

        for (i in 0..parameterFields.size) {
            parametersSheet.autoSizeColumn(i)
        }
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        insertResultsSheet(workbook)
        insertParametersSheet(workbook)

        val fileOut = FileOutputStream(outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
