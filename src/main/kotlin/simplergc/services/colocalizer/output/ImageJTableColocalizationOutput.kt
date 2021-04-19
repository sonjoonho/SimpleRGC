package simplergc.services.colocalizer.output

import kotlin.math.roundToInt
import org.scijava.ui.UIService
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.BaseRow
import simplergc.services.DoubleField
import simplergc.services.FieldRow
import simplergc.services.ImageJTableWriter
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.StringField
import simplergc.services.Table

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class ImageJTableColocalizationOutput(
    transductionParameters: Parameters.Transduction,
    val result: TransductionResult,
    uiService: UIService
) : ColocalizationOutput(transductionParameters) {

    override val tableWriter = ImageJTableWriter(uiService)

    private val table = Table().apply { addRow(FieldRow(listOf(
        "Label",
        "Count",
        "Area",
        "Median",
        "Mean",
        "Integrated Density",
        "Raw Integrated Density"
    ).map { StringField(it) })) }

    data class Row(
        val label: String,
        val count: Int = 0,
        val area: Int = 0,
        val median: Int = 0,
        val mean: Double = 0.0,
        val integratedDensity: Double = 0.0,
        val rawIntegratedDensity: Int = 0
    ) : BaseRow {
        override fun toList() = listOf(
            StringField(label),
            IntField(count),
            IntField(area),
            IntField(median),
            DoubleField(mean),
            DoubleField(integratedDensity),
            IntField(rawIntegratedDensity)
        )
    }

    override fun writeSummary() {
        table.addRow(Row(label = "--- Summary ---", count = result.targetCellCount))
        table.addRow(Row(label = "Total number of cells in cell morphology channel", count = result.targetCellCount))

        table.addRow(Row(label = "Transduced cells", count = result.overlappingOverlaidCells.size))

        table.addRow(
            Row(
                label = "Transduction Efficiency (rounded to nearest integer) %",
                count = result.transductionEfficiency.roundToInt()
            )
        )
        table.addRow(
            Row(
                label = "Mean intensity of colocalized cells",
                count = (result.channelResults[transducedChannel - 1].cellAnalyses.sumByDouble { it.mean } / result.channelResults[transducedChannel - 1].cellAnalyses.size).toInt()))
    }

    override fun getSummaryTable(): Table {
        throw NotImplementedError("Summary table not available for ImageJ output")
    }

    override fun writeSummaryWithAggregates() {
        throw NotImplementedError("Summary table not available for ImageJ output")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            table.addRow(Row(label = "--- Cell Analysis, $name ---"))

            // Construct column values using the channel analysis values.
            result.channelResults[idx].cellAnalyses.forEachIndexed { i, cell ->
                table.addRow(
                    Row(
                        "Cell ${i + 1}",
                        1,
                        cell.area,
                        cell.median,
                        cell.mean,
                        cell.area * cell.mean,
                        cell.rawIntDen
                    )
                )
            }
        }
    }

    override fun writeParameters() {
        throw NotImplementedError("Parameters not available for ImageJ output")
    }

    override fun writeDocumentation() {
        throw NotImplementedError("Documentation not available for ImageJ output")
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Number>>,
        spaces: Int,
        startRow: Int
    ): AggregateRow {
        throw NotImplementedError("Aggregate not available for ImageJ output")
    }

    override fun output() {
        writeSummary()
        table.addRow(Row(label = "----------------------------------------------"))
        writeAnalysis()
        tableWriter.produce(table, "")
    }
}
