package simplergc.services.counter.output

import simplergc.services.BaseRow
import simplergc.services.DoubleField
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.SimpleOutput.Companion.PLUGIN_VERSION
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.counter.output.CounterOutput.Companion.PLUGIN_NAME

class CSVCounterOutput(private val counterParameters: Parameters.CounterParameters) : CounterOutput {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()
    private val csv = Table(arrayOf(
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
        override fun toFieldArray(): Array<Field> = arrayOf(
            StringField(fileName),
            IntField(cellCount),
            StringField(pluginName),
            StringField(pluginVersion),
            IntField(morphologyChannel),
            DoubleField(smallestCellDiameter),
            DoubleField(largestCellDiameter),
            IntField(localThresholdRadius),
            DoubleField(gaussianBlurSigma)
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
                morphologyChannel = counterParameters.targetChannel,
                smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                largestCellDiameter = counterParameters.cellDiameterRange.largest,
                localThresholdRadius = counterParameters.localThresholdRadius,
                gaussianBlurSigma = counterParameters.gaussianBlurSigma
                ))
        }

        csv.produceCSV(counterParameters.outputFile)
    }
}
