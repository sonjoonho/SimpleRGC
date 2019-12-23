package simplecolocalization.services.counter.output

import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput() {

    private val table: DefaultGenericTable = DefaultGenericTable()
    private val countColumn: IntColumn = IntColumn()

    override fun addCountForFile(count: Int, file: String) {
        countColumn.add(count)

        // TODO: Implement storing and displaying filenames
    }

    override fun output() {
        show()
    }

    fun show() {
        table.add(countColumn)
        table.setColumnHeader(0, "Count")
        uiService.show(table)
    }
}
