package simplergc.services

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.ui.UIService

/**
 * Outputs the result of the plugin.
 */
interface SimpleOutput {

    companion object {
        const val ARTICLE_CITATION = "[insert full citation]"
        const val PLUGIN_VERSION = "1.0.0"
    }

    fun output()
}

interface BaseRow {
    fun toList(): List<Field<*>>
}

sealed class Field<V>(val value: V)
class StringField(value: String) : Field<String>(value)
class IntField(value: Int) : Field<Int>(value)
class DoubleField(value: Double) : Field<Double>(value)
class BooleanField(value: Boolean) : Field<Boolean>(value)

class Table(private val schema: List<String>) {
    // data is a rows x columns representation of a table.
    val data: MutableList<List<Field<*>>> =
        if (schema.isNullOrEmpty()) mutableListOf() else mutableListOf(schema.map { StringField(it) })

    fun addRow(row: BaseRow) {
        data.add(row.toList())
    }

    fun produceImageJTable(uiService: UIService) {
        val table = DefaultGenericTable()
        var columns: List<DefaultColumn<String>> = listOf()
        val header = data[0]
        val body = data.drop(1)

        if (!schema.isNullOrEmpty()) {
            columns = header.map { DefaultColumn(String::class.java, it.toString()) }
        }

        for (row in body) {
            for (colIdx in row.indices) {
                columns[colIdx].add(row[colIdx].toString())
            }
        }

        table.addAll(columns)
        uiService.show(table)
    }

    fun produceCsv(file: File) {
        CsvWriter().write(file, StandardCharsets.UTF_8, data.map { row -> row.map { it.value.toString() }.toTypedArray() })
    }

    fun produceXlsx(workbook: XSSFWorkbook, sheetName: String) {
        val currSheet = workbook.createSheet(sheetName)
        var rowNum = 0

        val header = data[0]

        // Set the header.
        if (!schema.isNullOrEmpty()) {
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerFont.color = IndexedColors.BLUE.index
            val headerCellStyle = workbook.createCellStyle()
            headerCellStyle.setFont(headerFont)
            val headerRow = currSheet.createRow(rowNum)

            for (i in header.indices) {
                val cell = headerRow.createCell(i)
                cell.cellStyle = headerCellStyle
                cell.setCellValue(header[i].toString())
            }
            rowNum = 1
        }

        val body = data.drop(rowNum)

        for (row in body) {
            val currRow = currSheet.createRow(rowNum)
            for (i in row.indices) {
                val currCell = currRow.createCell(i)
                when (val f = row[i]) {
                    is StringField -> currCell.setCellValue(f.value)
                    is IntField -> currCell.setCellValue(f.value.toDouble()) // Does not support Ints.
                    is DoubleField -> currCell.setCellValue(f.value)
                    is BooleanField -> currCell.setCellValue(f.value)
                }
            }
            rowNum++
        }
        if (data.isNotEmpty()) {
            for (i in header.indices) {
                currSheet.autoSizeColumn(i)
            }
        }
    }
}

sealed class Parameters {
    data class CounterParameters(
        val outputFile: File,
        val targetChannel: Int,
        val cellDiameterRange: CellDiameterRange,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : Parameters()

    data class TransductionParameters(
        val outputFile: File,
        val shouldRemoveAxonsFromTargetChannel: Boolean,
        val transducedChannel: Int,
        val shouldRemoveAxonsFromTransductionChannel: Boolean,
        val cellDiameterText: String,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double,
        val targetChannel: Int
    ) : Parameters()
}
