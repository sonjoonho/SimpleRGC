package simplecolocalization.services.counter.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets

class CSVCounterOutput(private val outputFile: File) : CounterOutput() {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    /**
     * Saves count results into csv file at specified output path.
     */
    override fun output() {
        val csvWriter = CsvWriter()

        val outputData = java.util.ArrayList<Array<String>>()
        outputData.add(arrayOf("File Name", "Cell Count"))
        outputData.addAll(fileNameAndCountList.map { arrayOf(it.first.replace(",", ""), it.second.toString()) })

        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
