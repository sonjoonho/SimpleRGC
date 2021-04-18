package simplergc.services

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.ui.UIService

private const val UTF_8_BOM = "\ufeff"

/**
 * Table represents data in terms of rows and columns.
 */
class Table {
    // data is a rows x columns representation of a table.
    val data: MutableList<List<Field<*>>> = mutableListOf()

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

        var rowNum = 0
        // Set the header style.
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.BLUE.index
        val headerCellStyle = workbook.createCellStyle()
        headerCellStyle.setFont(headerFont)

        // Used for horizontally merged headers.
        val centeredHeaderStyle = workbook.createCellStyle()
        centeredHeaderStyle.setFont(headerFont)
        centeredHeaderStyle.setAlignment(HorizontalAlignment.CENTER)

        val decimalCellStyle = workbook.createCellStyle()
        decimalCellStyle.dataFormat = workbook.createDataFormat().getFormat("0.000")

        var maxCols = 0

        for (row in data) {
            val currRow = currSheet.createRow(rowNum)
            var colNum = 0
            for (i in row.indices) {
                val field = row[i]
                val currCell = currRow.createCell(colNum)
                when (field) {
                    is StringField -> currCell.setCellValue(field.value)
                    is IntField -> currCell.setCellValue(field.value.toDouble()) // Does not support Ints.
                    is DoubleField -> {
                        currCell.cellStyle = decimalCellStyle
                        currCell.setCellValue(field.value)
                    }
                    is BooleanField -> currCell.setCellValue(field.value)
                    is DoubleFormulaField -> {
                        currCell.cellStyle = decimalCellStyle
                        currCell.setCellType(CellType.FORMULA)
                        currCell.cellFormula = field.value
                    }
                    is IntFormulaField -> {
                        currCell.setCellType(CellType.FORMULA)
                        currCell.cellFormula = field.value
                    }
                    is HeaderField -> {
                        currCell.cellStyle = headerCellStyle
                        currCell.setCellValue(field.value)
                    }
                    is HorizontallyMergedHeaderField -> {
                        currCell.cellStyle = centeredHeaderStyle
                        currCell.setCellValue(field.value)
                        currSheet.addMergedRegion(CellRangeAddress(rowNum, rowNum, colNum, colNum + field.columnSpan - 1))
                        colNum += field.columnSpan - 1
                    }
                    is VerticallyMergedHeaderField -> {
                        currCell.cellStyle = headerCellStyle
                        currCell.setCellValue(field.value)
                        currSheet.addMergedRegion(CellRangeAddress(rowNum, rowNum + field.rowSpan - 1, colNum, colNum))
                    }
                }
                colNum++
            }
            maxCols = max(maxCols, colNum)
            rowNum++
        }
        for (i in 0 until maxCols) {
            currSheet.autoSizeColumn(i, true)
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

        if (data.isEmpty()) {
            return
        }

        val header = data[0]
        val body = data.drop(1)

        // Use first row as schema
        val columns = header.map { DefaultColumn(String::class.java, it.value.toString()) }

        for (row in body) {
            for (colIdx in row.indices) {
                columns[colIdx].add(row[colIdx].value.toString())
            }
        }

        imageJTable.addAll(columns)
        uiService.show(imageJTable)
    }
}

interface BaseRow {
    fun toList(): List<Field<*>>
}

// A basic row of fields
data class FieldRow(val fields: List<Field<*>>) : BaseRow {
    override fun toList(): List<Field<*>> = fields
}

class EmptyRow : BaseRow {
    override fun toList() = emptyList<Field<*>>()
}

// A MetricRow is a row for a given cell in a given file. The parameter metrics is nullable because not all columns are
// of equal length so fields can be null.
data class MetricRow(val rowIdx: Int, val metrics: List<Field<*>>) : BaseRow {
    override fun toList(): List<Field<*>> {
        return listOf(IntField(rowIdx)) + metrics
    }
}

data class AggregateRow(val name: String, val values: List<Field<*>>, val spaces: Int = 0) : BaseRow {
    override fun toList(): List<Field<*>> {
        // Spaces allows for the row to start at a later column
        return MutableList(spaces) { StringField("") } + listOf(StringField(name)) + values
    }
}

sealed class Field<V>(val value: V)
class StringField(value: String) : Field<String>(value)
class IntField(value: Int) : Field<Int>(value)
class DoubleField(value: Double) : Field<Double>(value)
class BooleanField(value: Boolean) : Field<Boolean>(value)
class IntFormulaField(value: String) : Field<String>(value)
class DoubleFormulaField(value: String) : Field<String>(value)
class HeaderField(value: String) : Field<String>(value)
class HorizontallyMergedHeaderField(field: HeaderField, val columnSpan: Int) : Field<String>(field.value) {
    init {
        if (columnSpan < 0) {
            throw IllegalArgumentException("Cannot have negative span for merged header")
        }
    }
}

class VerticallyMergedHeaderField(field: HeaderField, val rowSpan: Int) : Field<String>(field.value) {
    init {
        if (rowSpan < 0) {
            throw IllegalArgumentException("Cannot have negative span for merged header")
        }
    }
}

enum class Aggregate(val abbreviation: String, val generateValue: (AggregateGenerator) -> Field<*>) {
    Mean("Mean", AggregateGenerator::generateMean),
    StandardDeviation("Std Dev", AggregateGenerator::generateStandardDeviation),
    StandardErrorOfMean("SEM", AggregateGenerator::generateStandardErrorOfMean),
    Count("N", AggregateGenerator::generateCount)
}

abstract class AggregateGenerator {
    abstract fun generateMean(): Field<*>
    abstract fun generateStandardDeviation(): Field<*>
    abstract fun generateStandardErrorOfMean(): Field<*>
    abstract fun generateCount(): Field<*>
}

class XlsxAggregateGenerator(startRow: Int, column: Char, numCells: Int) : AggregateGenerator() {

    private val endCellRow = numCells + startRow - 1
    private val cellRange = "$column$startRow:$column$endCellRow"

    override fun generateMean(): Field<*> {
        return DoubleFormulaField("AVERAGE($cellRange)")
    }

    override fun generateStandardDeviation(): Field<*> {
        return DoubleFormulaField("STDEV($cellRange)")
    }

    override fun generateStandardErrorOfMean(): Field<*> {
        return DoubleFormulaField("STDEV($cellRange)/SQRT(COUNT($cellRange))")
    }

    override fun generateCount(): Field<*> {
        return IntFormulaField("COUNT($cellRange)")
    }
}

class CsvAggregateGenerator(val values: List<Number>) : AggregateGenerator() {

    private fun computeStandardDeviation(): Double {
        val squareOfMean = values.map { it.toDouble() }.average().pow(2)
        val meanOfSquares = values.map { it.toDouble().pow(2) }.average()
        return sqrt(meanOfSquares - squareOfMean)
    }

    override fun generateMean(): Field<*> {
        return DoubleField(values.map { it.toDouble() }.average())
    }

    override fun generateStandardDeviation(): Field<*> {
        return DoubleField(computeStandardDeviation())
    }

    override fun generateStandardErrorOfMean(): Field<*> {
        return DoubleField(computeStandardDeviation() / sqrt(values.size.toDouble()))
    }

    override fun generateCount(): Field<*> {
        return IntField(values.size)
    }
}
