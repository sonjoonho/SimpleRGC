package simplergc.commands.batch.output

import simplergc.services.Parameters
import simplergc.services.colocalizer.output.XLSXColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXLSXColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    XLSXColocalizationOutput(transductionParameters)
