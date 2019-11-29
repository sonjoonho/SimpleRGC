package simplecolocalization.services.counter.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.ArrayList

class CSVCounterOutput(private val count: Int, private val file: File) : CounterOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        val outputData = ArrayList<Array<String>>()
        outputData.add(arrayOf(count.toString()))
        csvWriter.write(file, StandardCharsets.UTF_8, outputData)
    }
}
