package simplecolocalization.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import simplecolocalization.commands.SimpleColocalization.TransductionResult
import simplecolocalization.services.SimpleOutput

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
        val outputData = ArrayList<Array<String>>()
        outputData.add(arrayOf("Label", "Count", "Area", "Median", "Mean", "Integrated Density", "Raw Integrated Density"))

        // Summaries
        outputData.add(arrayOf("--- Summary ---", "", "", ""))
        outputData.add(arrayOf("Total number of cells in cell morphology channel 1", result.targetCellCount.toString(), "", "", "", "", ""))
        outputData.add(arrayOf("Transduced cells in channel 1", result.overlappingTwoChannelCells.size.toString(), "", "", "", "", ""))
        val transductionEfficiency = (result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100
        outputData.add(arrayOf("Transduction Efficiency (%)", transductionEfficiency.toString(), "", "", "", "", ""))
        outputData.add(arrayOf("Mean intensity of colocalized cells", (result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size).toString(), "", "", "", "", ""))
        outputData.add(arrayOf("--- Transduced Channel Analysis, Colocalized Cells ---", "", "", "", "", ""))

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
