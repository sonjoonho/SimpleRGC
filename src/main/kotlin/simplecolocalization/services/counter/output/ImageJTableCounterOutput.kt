package simplecolocalization.services.counter.output

import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService

class ImageJTableCounterOutput(private val count: Int, private val uiService: UIService) : CounterOutput() {

    override fun output() {
        val table = DefaultGenericTable()
        val countColumn = IntColumn()
        countColumn.add(count)
        table.add(countColumn)
        table.setColumnHeader(0, "Count")
        uiService.show(table)
    }
}
