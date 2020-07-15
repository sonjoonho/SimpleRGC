package simplergc.services

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
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
    fun toStringArray(): Array<String>
}

interface Table {
    val data: Any
    fun addRow(row: BaseRow)
    fun produce()
}

class ImageJTable(private val schema: Array<String>, private val uiService: UIService) : Table {

    private val table: DefaultGenericTable = DefaultGenericTable()
    override val data: Map<String, ArrayList<String>> = schema.map { it to arrayListOf<String>() }.toMap()

    override fun addRow(row: BaseRow) {
        val rowStringArray = row.toStringArray()
        for (i in rowStringArray.indices) {
            (data[schema[i]] ?: error("Field does not exist in schema")).add(rowStringArray[i])
        }
    }

    override fun produce() {
        data.forEach {
            val column = DefaultColumn(String::class.java, it.key)
            column.addAll(it.value)
            table.add(column)
        }
        uiService.show(table)
    }
}

class CSV(private val file: File, schema: Array<String>?) : Table {

    override val data: ArrayList<Array<String>> = if (schema != null) arrayListOf(schema) else arrayListOf()
    private val csvWriter: CsvWriter = CsvWriter()

    override fun addRow(row: BaseRow) {
        data.add(row.toStringArray())
    }

    override fun produce() {
        csvWriter.write(file, StandardCharsets.UTF_8, data)
    }
}
