package simplergc.services.counter.output

import java.io.File
import simplergc.services.BaseRow
import simplergc.services.CSV
import simplergc.services.CellDiameterRange
import simplergc.services.SimpleOutput.Companion.PLUGIN_VERSION
import simplergc.services.counter.output.CounterOutput.Companion.PLUGIN_NAME

class CSVCounterOutput(
    outputFile: File,
    private val morphologyChannel: Int,
    private val cellDiameterRange: CellDiameterRange,
    private val localThresholdRadius: Int,
    private val gaussianBlurSigma: Double
) : CounterOutput {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()
    private val csv: CSV = CSV(outputFile, arrayOf(
        "File Name",
        "Cell Count",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    ))

    data class Row(
        val fileName: String,
        val cellCount: Int,
        val pluginName: String = PLUGIN_NAME,
        val pluginVersion: String = PLUGIN_VERSION,
        val morphologyChannel: Int,
        val smallestCellDiameter: Double,
        val largestCellDiameter: Double,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(
            fileName,
            cellCount.toString(),
            pluginName,
            pluginVersion,
            morphologyChannel.toString(),
            smallestCellDiameter.toString(),
            largestCellDiameter.toString(),
            localThresholdRadius.toString(),
            gaussianBlurSigma.toString()
        )
    }

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    /**
     * Saves count results into csv file at specified output path.
     */
    override fun output() {
        fileNameAndCountList.forEach {
            csv.addRow(Row(
                fileName = it.first.replace(",", ""),
                cellCount = it.second,
                morphologyChannel = morphologyChannel,
                smallestCellDiameter = cellDiameterRange.smallest,
                largestCellDiameter = cellDiameterRange.largest,
                localThresholdRadius = localThresholdRadius,
                gaussianBlurSigma = gaussianBlurSigma
                ))
        }

        csv.produce()
    }
}
