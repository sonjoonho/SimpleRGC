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

interface BaseRow {
    fun toList(): List<Field>
}

enum class FieldType {
    STRING, INT, DOUBLE, BOOLEAN
}

open class Field(val type: FieldType, private val value: Any) {
    override fun toString(): String {
        return value.toString()
    }
}

class StringField(val value: String) : Field(FieldType.STRING, value)
class IntField(val value: Int) : Field(FieldType.INT, value)
class DoubleField(val value: Double) : Field(FieldType.DOUBLE, value)
class BooleanField(val value: Boolean) : Field(FieldType.BOOLEAN, value)

class Table(private val schema: Array<String>?) {
    val data: MutableList<List<Field>> = if (schema.isNullOrEmpty()) mutableListOf() else mutableListOf(schema.map { StringField(it) as Field })

    fun addRow(row: BaseRow) {
        data.add(row.toList())
    }

    fun produceImageJTable(uiService: UIService) {
        val table = DefaultGenericTable()
        var columns: List<DefaultColumn<String>> = listOf()
        if (!schema.isNullOrEmpty()) {
            columns = data[0].map { DefaultColumn(String::class.java, it.toString()) }
        }
        data.drop(1).forEach {
            for (i in it.indices) {
                columns[i].add(it[i].toString())
            }
        }
        table.addAll(columns)
        uiService.show(table)
    }

    fun produceCSV(file: File) {
        CsvWriter().write(file, StandardCharsets.UTF_8, data.map { it.map { it.toString() }.toTypedArray() })
    }

    fun produceXLSX(workbook: XSSFWorkbook, sheetName: String) {
        val currSheet = workbook.createSheet(sheetName)
        var rowNum = 0
        if (!schema.isNullOrEmpty()) {
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerFont.color = IndexedColors.BLUE.index
            val headerCellStyle = workbook.createCellStyle()
            headerCellStyle.setFont(headerFont)
            val headerRow = currSheet.createRow(rowNum)
            for (i in data[0].indices) {
                val cell = headerRow.createCell(i)
                cell.cellStyle = headerCellStyle
                cell.setCellValue(data[0][i].toString())
            }
            rowNum = 1
        }
        for (row in data.drop(rowNum)) {
            val currRow = currSheet.createRow(rowNum)
            for (i in row.indices) {
                val currCell = currRow.createCell(i)
                when (row[i].type) {
                    FieldType.STRING -> currCell.setCellValue(row[i].toString())
                    FieldType.INT -> currCell.setCellValue(row[i].toString().toDouble())
                    FieldType.DOUBLE -> currCell.setCellValue(row[i].toString().toDouble())
                    FieldType.BOOLEAN -> currCell.setCellValue(row[i].toString().toBoolean())
                }
            }
            rowNum++
        }
        if (data.isNotEmpty()) {
            for (i in data[0].indices) {
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
