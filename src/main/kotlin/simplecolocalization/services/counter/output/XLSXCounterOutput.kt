package simplecolocalization.services.counter.output

import java.io.File
import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class XLSXCounterOutput(private val outputFile: File) : CounterOutput() {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    private val columns = arrayOf("File Name", "Cell Count")

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
            row.createCell(0).setCellValue(pair.first.replace(",", ""))
            val countCell = row.createCell(1)
            countCell.setCellValue(pair.second.toDouble())
            countCell.cellStyle = numberCellStyle
        }

        sheet.autoSizeColumn(0)

        val fileOut = FileOutputStream(outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
