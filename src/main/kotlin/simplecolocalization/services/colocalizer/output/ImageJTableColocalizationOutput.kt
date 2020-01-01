package simplecolocalization.services.colocalizer.output

import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplecolocalization.commands.SimpleColocalization
import simplecolocalization.services.CellColocalizationService
import simplecolocalization.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    val result: SimpleColocalization.TransductionResult,
    val uiService: UIService
) : SimpleOutput() {

    override fun output() {
        val table = DefaultGenericTable()

        val labelColumn = DefaultColumn(String::class.java)
        val countColumn = IntColumn()
        val areaColumn = IntColumn()
        val medianColumn = IntColumn()
        val meanColumn = IntColumn()

        // Summary columns
        labelColumn.add("Total no. target cells")
        countColumn.add(result.targetCellCount)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)

        labelColumn.add("No. transduced cells overlapping target cells")
        countColumn.add(result.overlappingTwoChannelCells.size)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)

        if (result.overlappingThreeChannelCells != null) {
            labelColumn.add("No. cells overlapping all three channel cells")
            countColumn.add(result.overlappingThreeChannelCells.size)
            areaColumn.add(0)
            medianColumn.add(0)
            meanColumn.add(0)
        }

        labelColumn.add("Quantification of each transduced cells overlapping target cells below --")
        countColumn.add(0)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)

        // Construct column values using the channel analysis values.
        result.overlappingTransducedCellAnalyses.forEach {
            labelColumn.add("")
            countColumn.add(1)
            areaColumn.add(it.area)
            medianColumn.add(it.median)
            meanColumn.add(it.mean)
        }

        table.add(labelColumn)
        table.add(countColumn)
        table.add(areaColumn)
        table.add(medianColumn)
        table.add(meanColumn)

        table.setColumnHeader(0, "Label")
        table.setColumnHeader(1, "Count")
        table.setColumnHeader(2, "Area")
        table.setColumnHeader(3, "Median")
        table.setColumnHeader(4, "Mean")

        uiService.show(table)
    }
}
