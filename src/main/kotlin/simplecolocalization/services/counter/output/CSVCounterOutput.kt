package simplecolocalization.services.counter.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets

class CSVCounterOutput(private val outputFile: File) : CounterOutput() {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    fun save() {
        val csvWriter = CsvWriter()

        val outputData = fileNameAndCountList.map { arrayOf(it.first.replace(",", ""), it.second.toString()) }

        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
