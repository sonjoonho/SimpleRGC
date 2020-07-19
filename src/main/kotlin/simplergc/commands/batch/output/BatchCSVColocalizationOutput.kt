package simplergc.commands.batch.output

import simplergc.services.Parameters.TransductionParameters
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
        // TODO (tiger-cross): Add method for writing individual metrics CSVs
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
}
