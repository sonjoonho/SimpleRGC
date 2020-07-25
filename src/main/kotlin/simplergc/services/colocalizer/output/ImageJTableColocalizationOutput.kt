package simplergc.services.colocalizer.output

import org.scijava.ui.UIService
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.BaseRow
import simplergc.services.IntField
import simplergc.services.StringField
import simplergc.services.Table
import kotlin.math.roundToInt

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    val result: TransductionResult,
    private val uiService: UIService
) : ColocalizationOutput() {

    // TODO (131): Use fileNameAndResultsList in output

    private val table = Table(
        listOf(
            "Label",
            "Count",
            "Area",
            "Median",
            "Mean",
            "Integrated Density",
            "Raw Integrated Density"
        )
    )

    data class Row(
        val label: String,
        val count: Int = 0,
        val area: Int = 0,
        val median: Int = 0,
        val mean: Int = 0,
        val integratedDensity: Int = 0,
        val rawIntegratedDensity: Int = 0
    ) : BaseRow {
        override fun toList() = listOf(
            StringField(label),
            IntField(count),
            IntField(area),
            IntField(median),
            IntField(mean),
            IntField(integratedDensity),
            IntField(rawIntegratedDensity))
    }

    override fun output() {
        table.addRow(Row(label = "--- Summary ---", count = result.targetCellCount))

        table.addRow(Row(label = "Total number of cells in cell morphology channel 1", count = result.targetCellCount))

        table.addRow(Row(label = "Transduced cells in channel 1", count = result.overlappingTwoChannelCells.size))

        val transductionEfficiency = (result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100
        table.addRow(Row(label = "Transduction Efficiency (rounded to nearest integer) %", count = transductionEfficiency.roundToInt()))

        table.addRow(Row(label = "Mean intensity of colocalized cells", count = result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size))

        table.addRow(Row(label = "----------------------------------------------"))

        table.addRow(Row(label = "--- Transduced Channel Analysis, Colocalized Cells ---"))

        // Construct column values using the channel analysis values.
        result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cell ->
            table.addRow(Row("Cell ${i + 1}", 1, cell.area, cell.median, cell.mean, cell.area * cell.mean, cell.rawIntDen))
        }

        table.produceImageJTable(uiService)
    }
}
