package simplergc.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    private val result: TransductionResult,
    private val file: File
) : SimpleOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        // TODO: Figure out how to write each csv file in a folder.

        // TODO: Split into separate CSV files:
        // Summary
        val summaryData = ArrayList<Array<String>>()
        summaryData.add(
            arrayOf(
                "File Name",
                "Number of Cells",
                "Number of Transduced Cells",
                "Transduction Efficiency (%)",
                "Average Morphology Area (pixel^2)",
                "Mean Fluorescence Intensity (a.u.)",
                "Median Fluorescence Intensity (a.u.)",
                "Min Fluorescence Intensity (a.u.)",
                "Max Fluorescence Intensity (a.u.)",
                "IntDen",
                "RawIntDen"
            )
        )
        // TODO (tiger-cross): find a way to get input file name
        // TODO (tiger-cross): Calculate other metrics
        summaryData.add(
            arrayOf(
                "1",
                result.targetCellCount.toString(),
                result.overlappingTwoChannelCells.size.toString(),
                ((result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100).toString(),
                "TODO: Average Morphology Area",
                (result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size).toString(),
                "TODO: Median",
                "TODO: Min",
                "TODO: Max",
                "TODO: IntDen",
                "TODO: RawIntDen"
            )
        )

        // Per-cell analysis
        result.overlappingTransducedIntensityAnalysis.forEach {
            outputData.add(
                arrayOf(
                    "",
                    "1",
                    it.area.toString(),
                    it.median.toString(),
                    it.mean.toString(),
                    (it.mean * it.area).toString(),
                    it.sum.toString()
                )
            )
        }

        csvWriter.write(file, StandardCharsets.UTF_8, outputData)
    }
}
