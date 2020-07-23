package simplergc.commands.batch.output

import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.colocalizer.output.ColocalizationOutput

abstract class BatchColocalizationOutput : ColocalizationOutput() {

    protected val metricMappings = mapOf(
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

    val metricData = Table(
        arrayOf(
            "Transduced Cell" + fileNameAndResultsList.map { it.first }.toList()
        )
    )

    data class metricRow(val rowIdx: Int, val metrics: List<Int?>) : BaseRow {
        override fun toList(): List<Field> {
            val row = mutableListOf(IntField(rowIdx) as Field)
            row.addAll(metrics.map { StringField(it?.toString() ?: "") })
            return row.toList()
        }
    }
}