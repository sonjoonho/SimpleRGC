package simplergc.services.colocalizer.output

import kotlin.math.roundToInt
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplergc.commands.RGCTransduction.TransductionResult

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    val result: TransductionResult,
    val uiService: UIService
) : ColocalizationOutput() {

    // TODO (131): Use fileNameAndResultsList in output

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

        val transductionEfficiency = (result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100
        labelColumn.add("Transduction Efficiency (rounded to nearest integer) %")
        countColumn.add(transductionEfficiency.roundToInt())
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        labelColumn.add("Mean intensity of colocalized cells")
        countColumn.add(result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size)
        areaColumn.add(0)
        medianColumn.add(0)
        meanColumn.add(0)
        integratedDensityColumn.add(0)
        rawIntegratedDensityColumn.add(0)

        labelColumn.add("----------------------------------------------")
        countColumn.add(0)
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
            rawIntegratedDensityColumn.add(cell.rawIntDen)
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
