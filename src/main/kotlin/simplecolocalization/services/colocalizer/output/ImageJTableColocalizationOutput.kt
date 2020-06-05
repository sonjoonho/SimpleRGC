package simplecolocalization.services.colocalizer.output

import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplecolocalization.commands.SimpleColocalization
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
        val integratedDensityColumn = IntColumn()
        val rawIntegratedDensityColumn = IntColumn()

        // Summary columns
        labelColumn.add("--- Summary ---")
        countColumn.add(result.targetCellCount)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        labelColumn.add("Total number of cells in cell morphology channel 1")
        countColumn.add(result.targetCellCount)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        labelColumn.add("Transduced cells in channel 1")
        countColumn.add(result.overlappingTwoChannelCells.size)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        if (result.overlappingThreeChannelCells != null) {
            labelColumn.add("Transduced cells in both morphology channels")
            countColumn.add(result.overlappingThreeChannelCells.size)
            areaColumn.add(0)
            medianColumn.add(0)
            meanColumn.add(0)
            integratedDensityColumn.add(0)
            rawIntegratedDensityColumn.add(0)
        }

        labelColumn.add("Mean intensity of colocalized cells")
        countColumn.add(result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        labelColumn.add("--- Transduced Channel Analysis, Colocalized Cells ---")
        countColumn.add(0)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        // Construct column values using the channel analysis values.
        result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cell ->
            labelColumn.add("Cell ${i + 1}")
            countColumn.add(1)
            areaColumn.add(cell.area)
            medianColumn.add(cell.median)
            meanColumn.add(cell.mean)
            integratedDensityColumn.add(cell.area * cell.mean)
            rawIntegratedDensityColumn.add(cell.sum)
        }

        table.add(labelColumn)
        table.add(countColumn)
        table.add(areaColumn)
        table.add(medianColumn)
        table.add(meanColumn)
        table.add(integratedDensityColumn)
        table.add(rawIntegratedDensityColumn)

        table.setColumnHeader(0, "Label")
        table.setColumnHeader(1, "Count")
        table.setColumnHeader(2, "Area")
        table.setColumnHeader(3, "Median")
        table.setColumnHeader(4, "Mean")
        table.setColumnHeader(5, "Integrated Density")
        table.setColumnHeader(6, "Raw Integrated Density")

        uiService.show(table)
    }
}
