package simplergc.services.counter.output

import simplergc.services.BaseRow
import simplergc.services.DoubleField
import simplergc.services.IntField
import simplergc.services.Output
import simplergc.services.StringField
import simplergc.services.Table

/**
 * Outputs the result of cell counting.
 */
abstract class CounterOutput : Output {

    protected val fileNameAndCountList = mutableListOf<Pair<String, Int>>()

    companion object {
        const val PLUGIN_NAME = "RGC Counter"
    }

    fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    protected val resultsData = Table(listOf("File Name", "Cell Count"))
    data class ResultsRow(val fileName: String, val count: Int) : BaseRow {
        override fun toList() = listOf(StringField(fileName), IntField(count))
    }

    protected val parametersData = Table(
        listOf(
            "File Name",
            "Simple RGC Plugin",
            "Version",
            "Morphology Channel",
            "Smallest Cell Diameter (px)",
            "Largest Cell Diameter (px)",
            "Local Threshold Radius",
            "Gaussian Blur Sigma"
        )
    )
    data class ParametersRow(
        val fileName: String,
        val pluginName: String = PLUGIN_NAME,
        val pluginVersion: String = Output.PLUGIN_VERSION,
        val targetChannel: Int,
        val smallestCellDiameter: Double,
        val largestCellDiameter: Double,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : BaseRow {
        override fun toList() = listOf(
            StringField(fileName),
            StringField(pluginName),
            StringField(pluginVersion),
            IntField(targetChannel),
            DoubleField(smallestCellDiameter),
            DoubleField(largestCellDiameter),
            IntField(localThresholdRadius),
            DoubleField(gaussianBlurSigma)
        )
    }

    protected val parametersAndResultsData = Table(
        listOf(
            "File Name",
            "Cell Count",
            "Simple RGC Plugin",
            "Version",
            "Morphology Channel",
            "Smallest Cell Diameter (px)",
            "Largest Cell Diameter (px)",
            "Local Threshold Radius",
            "Gaussian Blur Sigma"
        )
    )

    data class ParametersResultsRow(
        val fileName: String,
        val cellCount: Int,
        val pluginName: String = PLUGIN_NAME,
        val pluginVersion: String = Output.PLUGIN_VERSION,
        val morphologyChannel: Int,
        val smallestCellDiameter: Double,
        val largestCellDiameter: Double,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : BaseRow {
        override fun toList() = listOf(
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
}
