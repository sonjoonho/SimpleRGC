package simplecolocalization.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import simplecolocalization.commands.SimpleColocalization

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    private val result: SimpleColocalization.TransductionResult,
    private val file: File
) : ColocalizationOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        val outputData = ArrayList<Array<String>>()
        outputData.add(arrayOf("Label", "Count", "Area", "Median", "Mean"))

        // Summaries
        outputData.add(arrayOf("Total no. target cells", result.targetCellCount.toString(), "", "", ""))
        outputData.add(arrayOf("No. transduced cells overlapping target cells", result.overlappingTwoChannelCells.size.toString(), "", "", ""))
        if (result.overlappingThreeChannelCells != null) {
            outputData.add(arrayOf("No. cells overlapping all three channels", result.overlappingThreeChannelCells.size.toString(), "", "", ""))
        }
        outputData.add(arrayOf("Quantification of each transduced cells overlapping target cells below --", "", "", ""))

        // Per-cell analysis
        result.overlappingTransducedCellAnalyses.forEach { outputData.add(arrayOf("", "1", it.area.toString(), it.median.toString(), it.mean.toString())) }

        csvWriter.write(file, StandardCharsets.UTF_8, outputData)
    }
}
