package simplecolocalization.services.counter.output

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplecolocalization.services.CellDiameterRange
import java.io.File
import java.io.FileOutputStream

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

        val headerRow = sheet.createRow(0)

        // Header
        for (col in columns.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(columns[col])
        }

        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")

        var rowIdx = 1
        for (pair in fileNameAndCountList) {
            val row = sheet.createRow(rowIdx++)

            // File Name
            row.createCell(0).setCellValue(pair.first.replace(",", ""))

            // Cell Count
            val countCell = row.createCell(1)
            countCell.setCellValue(pair.second.toDouble())

            // Parameters
            val morphologyChannelCell = row.createCell(2)
            morphologyChannelCell.setCellValue(morphologyChannel.toDouble())

            val smallestDiameterCell = row.createCell(3)
            smallestDiameterCell.setCellValue(cellDiameterRange.smallest)

            val largestDiameterCell = row.createCell(4)
            largestDiameterCell.setCellValue(cellDiameterRange.largest)

            val localThresholdRadiusCell = row.createCell(5)
            localThresholdRadiusCell.setCellValue(localThresholdRadius.toDouble())

            val gaussianBlurSigmaCell = row.createCell(6)
            gaussianBlurSigmaCell.setCellValue(gaussianBlurSigma)

        }

        for (i in 0..6) {
            sheet.autoSizeColumn(i)
        }

        val fileOut = FileOutputStream(outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
