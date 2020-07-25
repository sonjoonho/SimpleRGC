package simplergc.commands.batch.output

import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput

abstract class BatchColocalizationOutput() : ColocalizationOutput() {

    val documentationRows = listOf<DocumentationRow>(
        DocumentationRow("The article: ", "TODO: Insert citation"),
        DocumentationRow("", ""),
        DocumentationRow("Abbreviation", "Description"),
        DocumentationRow("Summary", "Key measurements per image"),
        DocumentationRow("Mean Int: ", "Mean fluorescence intensity for each transduced cell"),
        DocumentationRow("Median Int:", "Median fluorescence intensity for each transduced cell"),
        DocumentationRow("Min Int: ", "Min fluorescence intensity for each transduced cell"),
        DocumentationRow("Max Int: ", "Max fluorescence intensity for each transduced cell"),
        DocumentationRow("Raw IntDen:", "Raw Integrated Density for each transduced cell"),
        DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin")
    )

    // metricMappings returns a map from metric name to a list of [filenames and a list of values].
    fun metricMappings(): Map<String, List<Pair<String, List<Int>>>> {
        val areasWithFilenames = mutableListOf<Pair<String, List<Int>>>()
        val meansWithFilenames = mutableListOf<Pair<String, List<Int>>>()
        val mediansWithFilenames = mutableListOf<Pair<String, List<Int>>>()
        val minsWithFilenames = mutableListOf<Pair<String, List<Int>>>()
        // I am aware this is not an actual word don't @ me.
        val maxsWithFilenames = mutableListOf<Pair<String, List<Int>>>()
        val intDensWithFilenames = mutableListOf<Pair<String, List<Int>>>()

        for ((filename, result) in fileNameAndResultsList) {
            val areas = mutableListOf<Int>()
            val means = mutableListOf<Int>()
            val medians = mutableListOf<Int>()
            val mins = mutableListOf<Int>()
            val maxs = mutableListOf<Int>()
            val intDens = mutableListOf<Int>()

            for (cell in result.overlappingTransducedIntensityAnalysis) {
                areas.add(cell.area)
                means.add(cell.mean)
                medians.add(cell.median)
                mins.add(cell.min)
                maxs.add(cell.max)
                intDens.add(cell.rawIntDen)
            }
            areasWithFilenames.add(Pair(filename, areas))
            meansWithFilenames.add(Pair(filename, means))
            mediansWithFilenames.add(Pair(filename, medians))
            minsWithFilenames.add(Pair(filename, mins))
            maxsWithFilenames.add(Pair(filename, maxs))
            intDensWithFilenames.add(Pair(filename, intDens))
        }
        return mapOf(
            "Morphology Area" to areasWithFilenames,
            "Mean Int" to meansWithFilenames,
            "Median Int" to mediansWithFilenames,
            "Min Int" to minsWithFilenames,
            "Max Int" to maxsWithFilenames,
            "Raw IntDen" to intDensWithFilenames
        )
    }

    fun maxRows(): Int {
        val results = fileNameAndResultsList.unzip().second
        val sizes = results.map { it.overlappingTwoChannelCells.size }
        return sizes.max() ?: 0
    }

    fun metricData() =
        Table(listOf("Transduced Cell") + fileNameAndResultsList.map { (filename, _) -> filename })
}

// A MetricRow is a row for a given cell in a given file. The parameter metrics is nullable because not all columns are
// of equal length so fields can be null.
data class MetricRow(val rowIdx: Int, val metrics: List<Int?>) : BaseRow {
    override fun toList(): List<Field<*>> {
        val row = mutableListOf(IntField(rowIdx) as Field<*>)
        row.addAll(metrics.map { StringField(it?.toString() ?: "") })
        return row.toList()
    }
}