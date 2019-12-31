package simplecolocalization.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import simplecolocalization.services.CellColocalizationService
import simplecolocalization.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    private val analysis: Array<CellColocalizationService.CellAnalysis>,
    private val outputFile: File
) : SimpleOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        val outputData = ArrayList<Array<String>>()
        outputData.add(arrayOf("Area", "Median", "Mean"))
        analysis.forEach { outputData.add(arrayOf(it.area.toString(), it.median.toString(), it.mean.toString())) }
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
