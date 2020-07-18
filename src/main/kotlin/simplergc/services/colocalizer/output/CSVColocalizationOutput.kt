package simplergc.services.colocalizer.output

import java.io.File
import simplergc.services.Parameters
import simplergc.services.Table

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) : ColocalizationOutput() {

    private val outputPath: String = "${transductionParameters.outputFile.path}${File.separator}"

    override fun output() {
        writeDocumentationCsv()
        writeSummaryCsv()
        writeTransductionAnalysisCsv()
        writeParametersCsv()
    }

    private val documentationCsv = Table(arrayOf())

    private fun writeDocumentationCsv() {
        // Constant array of information
        documentationCsv.addRow(DocumentationRow("The Article: ", "TODO: insert full citation of manuscript when complete"))
        documentationCsv.addRow(DocumentationRow("", ""))
        documentationCsv.addRow(DocumentationRow("Abbreviation: ", "Description"))
        documentationCsv.addRow(DocumentationRow("Summary: ", "Key overall measurements per image"))
        documentationCsv.addRow(DocumentationRow("Transduced Cell Analysis: ", "Cell-by-cell metrics of transduced cells"))
        documentationCsv.addRow(DocumentationRow("Parameters: ", "Parameters used for SimpleRGC plugin"))
        documentationCsv.produceCSV(File("${outputPath}Documentation.csv"))
    }

    private fun writeSummaryCsv() {
        // Summary
        // TODO (#156): Add integrated density
        println("file name and results size: ${fileNameAndResultsList.size}")
        fileNameAndResultsList.forEach {
            summaryData.addRow(SummaryRow(it.first, it.second.getSummary()))
        }
        summaryData.produceCSV(File("${outputPath}Summary.csv"))
    }

    private fun writeTransductionAnalysisCsv() {
        fileNameAndResultsList.forEach {
            val fileName = it.first
            it.second.overlappingTransducedIntensityAnalysis.forEach { cellAnalysis ->
                transductionAnalysisData.addRow(TransductionAnalysisRow(fileName, cellAnalysis))
            }
        }
        transductionAnalysisData.produceCSV(File("${outputPath}Transduced Cell Analysis.csv"))
    }

    private fun writeParametersCsv() {
        // TODO (#156): Add pixel size (micrometers) in next sprint.
        fileNameAndResultsList.forEach {
            parametersData.addRow(
                ParametersRow(
                    it.first,
                    transductionParameters.targetChannel,
                    transductionParameters.shouldRemoveAxonsFromTargetChannel,
                    transductionParameters.transducedChannel,
                    transductionParameters.shouldRemoveAxonsFromTransductionChannel,
                    transductionParameters.cellDiameterText,
                    transductionParameters.localThresholdRadius,
                    transductionParameters.gaussianBlurSigma
                )
            )
        }
        parametersData.produceCSV(File("${outputPath}Parameters.csv"))
    }
}
