package simplergc.commands.batch.output

import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CSVColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCSVColocalizationOutput(transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val csvColocalizationOutput = CSVColocalizationOutput(transductionParameters)

    override fun output() {
        csvColocalizationOutput.checkOutputFolderCanBeCreated()
        csvColocalizationOutput.writeSummaryCsv()
        writeDocumentationCsv()
        for (metricName in getMetricMappings().keys) {
            writeMetricCSV(metricName)
        }
        csvColocalizationOutput.writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "The Article: ",
                "TODO: insert full citation of manuscript when complete"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(DocumentationRow("", ""))
        csvColocalizationOutput.documentationCsv.addRow(DocumentationRow("Abbreviation: ", "Description"))
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Summary: ",
                "Key overall measurements per image"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Morphology Area: ",
                "Average morphology area for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Mean Int: ",
                "Mean fluorescence intensity for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Median Int:",
                "Median fluorescence intensity for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Min Int: ",
                "Min fluorescence intensity for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Max Int: ",
                "Max fluorescence intensity for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Raw IntDen:",
                "Raw Integrated Density for each transduced cell"
            )
        )
        csvColocalizationOutput.documentationCsv.addRow(
            DocumentationRow(
                "Parameters: ",
                "Parameters used for SimpleRGC plugin"
            )
        )
        csvColocalizationOutput.documentationCsv.produceCSV(File("${csvColocalizationOutput.outputPath}Documentation.csv"))
    }

    private fun writeMetricCSV(metricName: String) {
        val maxRows =
            fileNameAndResultsList.maxBy { it.second.overlappingTwoChannelCells.size }?.second?.overlappingTwoChannelCells?.size
        for (rowIdx in 0..maxRows!!) {
            val rowData = getMetricMappings().getOrDefault(metricName, emptyList()).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(metricRow(rowIdx, rowData))
        }
        metricData.produceCSV(File("${csvColocalizationOutput.outputPath}${metricName}.csv"))
    }
}
