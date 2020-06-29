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

    private val resultColumns = arrayOf(
        "File Name",
        "Cell Count"
    )

    private val parameterColumns = arrayOf(
        "File Name",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    )

    private fun generateResultsSheet(workbook: XSSFWorkbook) {
        val results = workbook.createSheet("Results")

        val createHelper = workbook.creationHelper

        val articleRow = results.createRow(0)
        articleRow.createCell(0).setCellValue("The article:")
        articleRow.createCell(1).setCellValue("[insert full citation]")

        val headerRow = results.createRow(2)

        // Header
        for (col in resultColumns.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(resultColumns[col])
        }

        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")

        var rowIdx = 3
        for (pair in fileNameAndCountList) {
            val row = results.createRow(rowIdx++)

            // File Name
            row.createCell(0).setCellValue(pair.first.replace(",", ""))

            // Cell Count
            val countCell = row.createCell(1)
            countCell.setCellValue(pair.second.toDouble())
        }

        for (i in 0..3) {
            results.autoSizeColumn(i)
        }
    }

    private fun generateParametersSheet(workbook: XSSFWorkbook) {
        val parameters = workbook.createSheet("Parameters")

        val headerRow = parameters.createRow(0)

        // Header
        for (col in parameterColumns.indices) {
            val cell = headerRow.createCell(col)
            cell.setCellValue(parameterColumns[col])
        }

        var rowIdx = 1
        for (pair in fileNameAndCountList) {
            val row = parameters.createRow(rowIdx++)
            var colIdx = 0
            // File Name
            row.createCell(colIdx++).setCellValue(pair.first.replace(",", ""))

            // Plugin Information
            val pluginNameCell = row.createCell(colIdx++)
            pluginNameCell.setCellValue("RGC Counter")

            val pluginVersionCell = row.createCell(colIdx++)
            pluginVersionCell.setCellValue(1.0)

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


        for (i in 0..7) {
            parameters.autoSizeColumn(i)
        }
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        generateResultsSheet(workbook)
        generateParametersSheet(workbook)

        val fileOut = FileOutputStream(outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
