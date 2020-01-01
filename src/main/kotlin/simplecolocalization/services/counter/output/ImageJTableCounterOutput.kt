package simplecolocalization.services.counter.output

import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput() {

    private val table: DefaultGenericTable = DefaultGenericTable()
    private val countColumn: IntColumn = IntColumn()

    override fun addCountForFile(count: Int, file: String) {
        countColumn.add(count)

        // TODO(kz): Implement storing and displaying filenames
    }

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        table.add(countColumn)
        table.setColumnHeader(0, "Count")
        uiService.show(table)
    }
}
