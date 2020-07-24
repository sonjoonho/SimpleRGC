package simplergc.commands.batch.output

import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput

abstract class BatchColocalizationOutput : ColocalizationOutput() {

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

    fun getMetricMappings(): Map<String, List<Pair<String, List<Int>>>> = mapOf(
        "Morphology Area" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.area })
        },
        "Mean Int" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.mean })
        },
        "Median Int" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.median })
        },
        "Min Int" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.min })
        },
        "Max Int" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.max })
        },
        "Raw IntDen" to fileNameAndResultsList.map {
            Pair(
                it.first,
                it.second.overlappingTransducedIntensityAnalysis.map { cell -> cell.rawIntDen })
        }
    )

    fun getMaxRows() =
        fileNameAndResultsList.maxBy { it.second.overlappingTwoChannelCells.size }?.second?.overlappingTwoChannelCells?.size

    fun getMetricData() =
        Table((listOf("Transduced Cell") + fileNameAndResultsList.map { it.first }.toList()).toTypedArray())
}

data class metricRow(val rowIdx: Int, val metrics: List<Int?>) : BaseRow {
    override fun toList(): List<Field> {
        val row = mutableListOf(IntField(rowIdx) as Field)
        row.addAll(metrics.map { StringField(it?.toString() ?: "") })
        return row.toList()
    }
}
