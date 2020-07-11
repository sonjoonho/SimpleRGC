package simplergc.services.counter.output

import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplergc.services.BaseTable

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput {

    private val table: CounterTable = CounterTable(DefaultGenericTable())

    override fun addCountForFile(count: Int, file: String) {
        table.addRow(CounterTable.Row(count))

        // TODO(#131): Implement storing and displaying filenames
    }

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        uiService.show(table.produce())
    }

    class CounterTable(private val table: DefaultGenericTable) : BaseTable(table) {

        private val countColumn: IntColumn = IntColumn()

        data class Row(val count: Int = 0)

        init {
            countColumn.header = "Count"
        }

        fun addRow(row: Row) {
            countColumn.add(row.count)
        }

        override fun produce(): DefaultGenericTable {
            table.add(countColumn)
            return table
        }
    }
}
