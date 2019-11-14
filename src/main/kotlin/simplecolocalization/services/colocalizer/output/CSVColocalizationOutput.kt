package simplecolocalization.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import simplecolocalization.services.CellColocalizationService
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    private val analysis: Array<CellColocalizationService.CellAnalysis>,
    private val file: File
) : ColocalizationOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        val data = ArrayList<Array<String>>()
        data.add(arrayOf("Area", "Mean", "Min", "Max"))
        analysis.forEach { data.add(arrayOf(it.area.toString(), it.mean.toString(), it.min.toString(), it.max.toString())) }
        csvWriter.write(file, StandardCharsets.UTF_8, data)
    }
}
