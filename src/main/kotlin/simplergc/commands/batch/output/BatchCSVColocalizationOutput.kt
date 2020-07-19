package simplergc.commands.batch.output

import simplergc.services.Parameters.TransductionParameters
import simplergc.services.colocalizer.output.CSVColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchCSVColocalizationOutput(private val transductionParameters: TransductionParameters) :
    CSVColocalizationOutput(transductionParameters) {

    override fun output() {
        checkOutputFolderCanBeCreated()
        writeSummaryCsv()
        // TODO (tiger-cross): Add method for writing documentation csv
        // TODO (tiger-cross): Add method for writing individual metrics CSVs
        writeParametersCsv()
    }
}
