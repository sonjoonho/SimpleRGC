package simplergc.services.counter.output

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.File
import java.nio.charset.StandardCharsets
import simplergc.services.CellDiameterRange
import simplergc.services.SimpleOutput.Companion.PLUGIN_VERSION
import simplergc.services.counter.output.CounterOutput.Companion.PLUGIN_NAME

class CSVCounterOutput(
    private val outputFile: File,
    private val morphologyChannel: Int,
    private val cellDiameterRange: CellDiameterRange,
    private val localThresholdRadius: Int,
    private val gaussianBlurSigma: Double
) : CounterOutput {

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
        outputData.add(arrayOf(
            "File Name",
            "Cell Count",
            "Simple RGC Plugin",
            "Version",
            "Morphology Channel",
            "Smallest Cell Diameter (px)",
            "Largest Cell Diameter (px)",
            "Local Threshold Radius",
            "Gaussian Blur Sigma"))
        outputData.addAll(
            fileNameAndCountList.map { arrayOf(
                it.first.replace(",", ""),
                it.second.toString(),
                PLUGIN_NAME,
                PLUGIN_VERSION,
                morphologyChannel.toString(),
                cellDiameterRange.smallest.toString(),
                cellDiameterRange.largest.toString(),
                localThresholdRadius.toString(),
                gaussianBlurSigma.toString()
            ) })

        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
