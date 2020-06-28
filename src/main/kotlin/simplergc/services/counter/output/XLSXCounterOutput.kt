package simplergc.services.counter.output

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

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    private val columns = arrayOf(
        "File Name",
        "Cell Count",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    )

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        val sheet = workbook.createSheet("RGC Counter")

        val createHelper = workbook.creationHelper

        val articleRow = sheet.createRow(0)
        articleRow.createCell(0).setCellValue("The article:")
        articleRow.createCell(1).setCellValue("[insert full citation]")


        val headerRow = sheet.createRow(3)

        // Header
        for (col in columns.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(columns[col])
        }

        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")

        var rowIdx = 4
        for (pair in fileNameAndCountList) {
            var colIdx = 0
            val row = sheet.createRow(rowIdx++)

            // File Name
            row.createCell(colIdx++).setCellValue(pair.first.replace(",", ""))

            // Cell Count
            val countCell = row.createCell(colIdx++)
            countCell.setCellValue(pair.second.toDouble())

            // Plugin Information
            val pluginNameCell = row.createCell(colIdx++)
            pluginNameCell.setCellValue("RGC Counter")

            val pluginVersionCell = row.createCell(colIdx++)
            pluginVersionCell.setCellValue("1.0")

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

        for (i in 0..8) {
            sheet.autoSizeColumn(i)
        }

        val fileOut = FileOutputStream(outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
