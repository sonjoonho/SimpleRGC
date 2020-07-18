package simplergc.services.counter.output

import org.scijava.ui.UIService
import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.Table

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput {

    private val schema = arrayOf("Count")
    private val table: Table = Table(schema)

    data class Row(private val count: Int) : BaseRow {
        override fun toFieldArray(): Array<Field> = arrayOf(IntField(count))
    }

    override fun addCountForFile(count: Int, file: String) {
        table.addRow(Row(count))
        // TODO(#131): Implement storing and displaying filenames
    }

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        table.produceImageJTable(uiService)
    }
}
