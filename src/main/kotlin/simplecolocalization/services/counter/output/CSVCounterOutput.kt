package simplecolocalization.services.counter.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets

class CSVCounterOutput(private val outputFile: File) : CounterOutput() {

    private val arrayList: ArrayList<Pair<String, Int>> = ArrayList()

    override fun addCountForFile(count: Int, file: String) {
        arrayList.add(Pair(file, count))
    }

    fun save() {
        val csvWriter = CsvWriter()

        val outputData = arrayList.map { arrayOf(it.first, it.second.toString()) }

        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
