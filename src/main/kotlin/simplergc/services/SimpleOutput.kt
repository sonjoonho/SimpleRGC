package simplergc.services

import de.siegmar.fastcsv.writer.CsvWriter
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.ui.UIService
import java.io.File
import java.nio.charset.StandardCharsets

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

interface TableProducer {
    fun produce(table: Table, to: String)
}

class XlsxTableProducer(private val workbook: XSSFWorkbook) : TableProducer {
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

class CsvTableProducer : TableProducer {
    override fun produce(table: Table, to: String) {
        val data = table.data
        CsvWriter().write(
            File(to),
            StandardCharsets.UTF_8,
            data.map { row -> row.map { it.value.toString() }.toTypedArray() })
    }
}

class ImageJTableProducer(private val uiService: UIService) : TableProducer {

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

sealed class Field<V>(val value: V)
class StringField(value: String) : Field<String>(value)
class IntField(value: Int) : Field<Int>(value)
class DoubleField(value: Double) : Field<Double>(value)
class BooleanField(value: Boolean) : Field<Boolean>(value)

class Table(val schema: List<String>) {
    // data is a rows x columns representation of a table.
    val data: MutableList<List<Field<*>>> =
        if (schema.isNullOrEmpty()) mutableListOf() else mutableListOf(schema.map { StringField(it) })

    fun addRow(row: BaseRow) {
        data.add(row.toList())
    }
}

sealed class Parameters {
    data class Counter(
        val outputFile: File,
        val targetChannel: Int,
        val cellDiameterRange: CellDiameterRange,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : Parameters()

    data class Transduction(
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
