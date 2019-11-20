package simplecolocalization.services.colocalizer.output

import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplecolocalization.services.CellColocalizationService

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    val analysis: Array<CellColocalizationService.CellAnalysis>,
    val uiService: UIService
) : ColocalizationOutput() {

    override fun output() {
        val table = DefaultGenericTable()

        val areaColumn = IntColumn()
        val meanColumn = IntColumn()
        val minColumn = IntColumn()
        val maxColumn = IntColumn()

        // Construct column values using the channel analysis values.
        analysis.forEach {
            areaColumn.add(it.area)
            meanColumn.add(it.mean)
            minColumn.add(it.min)
            maxColumn.add(it.max)
        }

        table.add(areaColumn)
        table.add(meanColumn)
        table.add(minColumn)
        table.add(maxColumn)

        table.setColumnHeader(0, "Area")
        table.setColumnHeader(1, "Mean")
        table.setColumnHeader(2, "Min")
        table.setColumnHeader(3, "Max")
        uiService.show(table)
    }
}
