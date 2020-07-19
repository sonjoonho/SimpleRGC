package simplergc.services.colocalizer.output

import java.io.File
import java.io.IOException
import simplergc.services.Parameters
import simplergc.services.Table

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
open class CSVColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    ColocalizationOutput() {

    private val outputPath: String = "${transductionParameters.outputFile.path}${File.separator}"

    override fun output() {
        checkOutputFolderCanBeCreated()
        writeDocumentationCsv()
        writeSummaryCsv()
        writeTransductionAnalysisCsv()
        writeParametersCsv()
    }

    protected fun checkOutputFolderCanBeCreated() {
        val outputFileSuccess = File(transductionParameters.outputFile.path).mkdir()
        if (!outputFileSuccess and !transductionParameters.outputFile.exists()) {
            throw IOException()
        }
    }

    private val documentationCsv = Table(arrayOf())

    private fun writeDocumentationCsv() {
        // Constant array of information
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
                "Transduced Cell Analysis: ",
                "Cell-by-cell metrics of transduced cells"
            )
        )
        documentationCsv.addRow(DocumentationRow("Parameters: ", "Parameters used for SimpleRGC plugin"))
        documentationCsv.produceCSV(File("${outputPath}Documentation.csv"))
    }

    protected fun writeSummaryCsv() {
        // TODO (#156): Add integrated density
        for ((fileName, result) in fileNameAndResultsList) {
            summaryData.addRow(SummaryRow(fileName = fileName, summary = result.getSummary()))
        }
        summaryData.produceCSV(File("${outputPath}Summary.csv"))
    }

    private fun writeTransductionAnalysisCsv() {
        for ((fileName, result) in fileNameAndResultsList) {
            result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cellAnalysis ->
                // We index the cell number from 1.
                transductionAnalysisData.addRow(
                    TransductionAnalysisRow(
                        fileName = fileName,
                        transducedCell = i + 1,
                        cellAnalysis = cellAnalysis
                    )
                )
            }
        }
        transductionAnalysisData.produceCSV(File("${outputPath}Transduced Cell Analysis.csv"))
    }

    protected fun writeParametersCsv() {
        // TODO (#156): Add pixel size (micrometers) in next sprint.
        for ((fileName, _) in fileNameAndResultsList) {
            parametersData.addRow(
                ParametersRow(
                    fileName = fileName,
                    morphologyChannel = transductionParameters.targetChannel,
                    excludeAxonsFromMorphologyChannel = transductionParameters.shouldRemoveAxonsFromTargetChannel,
                    transductionChannel = transductionParameters.transducedChannel,
                    excludeAxonsFromTransductionChannel = transductionParameters.shouldRemoveAxonsFromTransductionChannel,
                    cellDiameterText = transductionParameters.cellDiameterText,
                    localThresholdRadius = transductionParameters.localThresholdRadius,
                    gaussianBlurSigma = transductionParameters.gaussianBlurSigma
                )
            )
        }
        parametersData.produceCSV(File("${outputPath}Parameters.csv"))
    }
}
