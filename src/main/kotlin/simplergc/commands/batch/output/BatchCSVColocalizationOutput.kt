package simplergc.commands.batch.output

import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.Parameters.TransductionParameters
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.colocalizer.output.CSVColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCSVColocalizationOutput(private val transductionParameters: TransductionParameters) :
    CSVColocalizationOutput(transductionParameters) {

    override fun output() {
        checkOutputFolderCanBeCreated()
        writeSummaryCsv()
        writeDocumentationCsv()
        for (metricName in metricMappings.keys) {
            writeMetricCSV(metricName)
        }
        writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        documentationCsv.addRow(
            DocumentationRow(
                "The Article: ",
                "TODO: insert full citation of manuscript when complete"
            )
        )
        documentationCsv.addRow(DocumentationRow("", ""))
        documentationCsv.addRow(DocumentationRow("Abbreviation: ", "Description"))
        documentationCsv.addRow(DocumentationRow("Summary: ", "Key overall measurements per image"))
        documentationCsv.addRow(
            DocumentationRow(
                "Morphology Area: ",
                "Average morphology area for each transduced cell"
            )
        )
        documentationCsv.addRow(DocumentationRow("Mean Int: ", "Mean fluorescence intensity for each transduced cell"))
        documentationCsv.addRow(
            DocumentationRow(
                "Median Int:",
                "Median fluorescence intensity for each transduced cell"
            )
        )
        documentationCsv.addRow(DocumentationRow("Min Int: ", "Min fluorescence intensity for each transduced cell"))
        documentationCsv.addRow(DocumentationRow("Max Int: ", "Max fluorescence intensity for each transduced cell"))
        documentationCsv.addRow(DocumentationRow("Raw IntDen:", "Raw Integrated Density for each transduced cell"))
        documentationCsv.addRow(DocumentationRow("Parameters: ", "Parameters used for SimpleRGC plugin"))
        documentationCsv.produceCSV(File("${outputPath}Documentation.csv"))
    }

    private val metricMappings = mapOf(
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

    private fun writeMetricCSV(metricName: String) {
        val maxRows =
            fileNameAndResultsList.maxBy { it.second.overlappingTwoChannelCells.size }?.second?.overlappingTwoChannelCells?.size
        for (rowIdx in 0..maxRows!!) {
            val rowData = metricMappings.getOrDefault(metricName, emptyList()).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(metricRow(rowIdx, rowData))
        }
        metricData.produceCSV(File("${outputPath}${metricName}.csv"))
    }
}
