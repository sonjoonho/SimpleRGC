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
        val medianColumn = IntColumn()
        val meanColumn = IntColumn()

        // Construct column values using the channel analysis values.
        analysis.forEach {
            areaColumn.add(it.area)
            medianColumn.add(it.median)
            meanColumn.add(it.mean)
        }

        table.add(areaColumn)
        table.add(medianColumn)
        table.add(meanColumn)

        table.setColumnHeader(0, "Area")
        table.setColumnHeader(1, "Median")
        table.setColumnHeader(2, "Mean")

        uiService.show(table)
    }
}
