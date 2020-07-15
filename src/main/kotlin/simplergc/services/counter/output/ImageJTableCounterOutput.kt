package simplergc.services.counter.output

import org.scijava.ui.UIService
import simplergc.services.BaseRow
import simplergc.services.ImageJTable
import simplergc.services.Table

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput {

    private val schema = arrayOf("Count")
    private val table: Table = ImageJTable(schema, uiService)

    data class Row(val count: Int) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(count.toString())
    }

    override fun addCountForFile(count: Int, file: String) {
        table.addRow(Row(count))
        // TODO(#131): Implement storing and displaying filenames
    }

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        table.produce()
    }
}
