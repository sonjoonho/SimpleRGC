package simplergc.services.counter.output

import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.BaseRow
import simplergc.services.DoubleField
import simplergc.services.HeaderField
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

    protected val resultsData = Table()

    /**
     * [spaces] is used by the CSV aggregate generator to avoid formatting issues.
     * [startRow] is only used by the XLSX aggregate generator.
     */
    abstract fun generateAggregateRow(
        aggregate: Aggregate,
        cellCounts: List<Number>,
        spaces: Int = 0,
        startRow: Int = 4
    ): AggregateRow

    abstract fun addTotalRow(t: Table, cellCounts: List<Int>)

    data class ResultsRow(val fileName: String, val count: Int) : BaseRow {
        override fun toList() = listOf(StringField(fileName), IntField(count))
    }

    protected val parameterHeadings = listOf(
        "File Name",
        "Cell Count",
        "Simple RGC Plugin",
        "Plugin Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    ).map { HeaderField(it) }

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
