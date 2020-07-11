package simplergc.services.colocalizer.output

import kotlin.math.roundToInt
import org.scijava.table.DefaultColumn
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.BaseTable

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    val result: TransductionResult,
    private val uiService: UIService
) : ColocalizationOutput {

    // TODO (131): Use fileNameAndResultsList in output

    private val table = ColocalizationTable(DefaultGenericTable())

    override fun output() {
        table.addRow(ColocalizationTable.Row(label = "--- Summary ---", count = result.targetCellCount))

        table.addRow(ColocalizationTable.Row(label = "Total number of cells in cell morphology channel 1", count = result.targetCellCount))

        table.addRow(ColocalizationTable.Row(label = "Transduced cells in channel 1", count = result.overlappingTwoChannelCells.size))

        val transductionEfficiency = (result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100
        table.addRow(ColocalizationTable.Row(label = "Transduction Efficiency (rounded to nearest integer) %", count = transductionEfficiency.roundToInt()))

        table.addRow(ColocalizationTable.Row(label = "Mean intensity of colocalized cells", count = result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size))

        table.addRow(ColocalizationTable.Row(label = "----------------------------------------------"))

        table.addRow(ColocalizationTable.Row(label = "--- Transduced Channel Analysis, Colocalized Cells ---"))

        // Construct column values using the channel analysis values.
        result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cell ->
            table.addRow(ColocalizationTable.Row("Cell ${i + 1}", 1, cell.area, cell.median, cell.mean, cell.area * cell.mean, cell.rawIntDen))
        }

        uiService.show(table.produce())
    }

    class ColocalizationTable(private val table: DefaultGenericTable) : BaseTable(table) {
        private val labelColumn = DefaultColumn(String::class.java)
        private val countColumn = IntColumn()
        private val areaColumn = IntColumn()
        private val medianColumn = IntColumn()
        private val meanColumn = IntColumn()
        private val integratedDensityColumn = IntColumn()
        private val rawIntegratedDensityColumn = IntColumn()

        data class Row(
            val label: String = "",
            val count: Int = 0,
            val area: Int = 0,
            val median: Int = 0,
            val mean: Int = 0,
            val integratedDensity: Int = 0,
            val rawIntegratedDensity: Int = 0
        )

        init {
            labelColumn.header = "Label"
            countColumn.header = "Count"
            areaColumn.header = "Area"
            medianColumn.header = "Median"
            meanColumn.header = "Mean"
            integratedDensityColumn.header = "Integrated Density"
            rawIntegratedDensityColumn.header = "Raw Integrated Density"
        }

        fun addRow(row: Row) {
            labelColumn.add(row.label)
            countColumn.add(row.count)
            areaColumn.add(row.area)
            medianColumn.add(row.median)
            meanColumn.add(row.mean)
            integratedDensityColumn.add(row.integratedDensity)
            rawIntegratedDensityColumn.add(row.rawIntegratedDensity)
        }

        override fun produce(): DefaultGenericTable {
            table.add(labelColumn)
            table.add(countColumn)
            table.add(areaColumn)
            table.add(medianColumn)
            table.add(meanColumn)
            table.add(integratedDensityColumn)
            table.add(rawIntegratedDensityColumn)

            return table
        }
    }
}
