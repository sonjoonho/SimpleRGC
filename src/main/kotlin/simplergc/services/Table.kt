package simplergc.services

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.ui.UIService

/**
 * Table represents data in terms of rows and columns.
 */
class Table(val schema: List<String>) {
    // data is a rows x columns representation of a table.
    val data: MutableList<List<Field<*>>> =
        if (schema.isNullOrEmpty()) mutableListOf() else mutableListOf(schema.map { StringField(it) })

    fun addRow(row: BaseRow) {
        data.add(row.toList())
    }
}

/**
 * TableWriter defines an object that can write a Table.
 */
interface TableWriter {
    fun produce(table: Table, to: String)
}

/**
 * XlsxTableWriter writes a Table as a sheet in a XSSFWorkbook.
 */
class XlsxTableWriter(private val workbook: XSSFWorkbook) : TableWriter {
    override fun produce(table: Table, to: String) {
        val currSheet = workbook.createSheet(to)
        val data = table.data
        val header = data[0]

        var rowNum = 0
        // Set the header.
        if (!table.schema.isNullOrEmpty()) {
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerFont.color = IndexedColors.BLUE.index
            val headerCellStyle = workbook.createCellStyle()
            headerCellStyle.setFont(headerFont)
            val headerRow = currSheet.createRow(rowNum)

            for (i in header.indices) {
                val cell = headerRow.createCell(i)
                cell.cellStyle = headerCellStyle
                cell.setCellValue(header[i].value.toString())
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

/**
 * CsvTableWriter writes a Table as a CSV file.
 */
class CsvTableWriter : TableWriter {
    override fun produce(table: Table, to: String) {
        val filename = FilenameUtils.removeExtension(to) ?: "Untitled"
        val file = File("$filename.csv")
        CsvWriter().write(
            file,
            StandardCharsets.UTF_8,
            table.data.map { row -> row.map { it.value.toString() }.toTypedArray() })
    }
}

/**
 * ImageJTableWriter writes a Table as an ImageJ table.
 */
class ImageJTableWriter(private val uiService: UIService) : TableWriter {

    // The to parameter is not used.
    override fun produce(table: Table, to: String) {
        val imageJTable = DefaultGenericTable()
        val data = table.data
        val header = data[0]
        val body = data.drop(1)

        var columns: List<DefaultColumn<String>> = listOf()
        if (!table.schema.isNullOrEmpty()) {
            columns = header.map { DefaultColumn(String::class.java, it.toString()) }
        }

        for (row in body) {
            for (colIdx in row.indices) {
                columns[colIdx].add(row[colIdx].toString())
            }
        }

        imageJTable.addAll(columns)
        uiService.show(imageJTable)
    }
}

interface BaseRow {
    fun toList(): List<Field<*>>
}

// A MetricRow is a row for a given cell in a given file. The parameter metrics is nullable because not all columns are
// of equal length so fields can be null.
data class MetricRow(val rowIdx: Int, val metrics: List<Int?>) : BaseRow {
    override fun toList(): List<Field<*>> {
        val row = mutableListOf(IntField(rowIdx) as Field<*>)
        row.addAll(metrics.map { if (it !== null) IntField(it) else StringField("") })
        return row
    }
}

sealed class Field<V>(val value: V)
class StringField(value: String) : Field<String>(value)
class IntField(value: Int) : Field<Int>(value)
class DoubleField(value: Double) : Field<Double>(value)
class BooleanField(value: Boolean) : Field<Boolean>(value)
