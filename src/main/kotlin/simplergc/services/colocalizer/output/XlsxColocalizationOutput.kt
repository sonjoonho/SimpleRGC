package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
open class XlsxColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    ColocalizationOutput() {

    override fun output() {
        val workbook = XSSFWorkbook()
        writeDocSheet(workbook)
        writeSummarySheet(workbook)
        writeTransductionAnalysisSheet(workbook)
        writeParamsSheet(workbook)

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        val docXlsx = Table(arrayOf())
        docXlsx.addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
        docXlsx.addRow(DocumentationRow("", ""))
        docXlsx.addRow(DocumentationRow("Abbreviation", "Description"))
        docXlsx.addRow(DocumentationRow("Summary", "Key measurements per image"))
        docXlsx.addRow(DocumentationRow("Transduced cells analysis", "Per-cell metrics of transduced cells"))
        docXlsx.addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
        docXlsx.produceXlsx(workbook, "Documentation")
    }

    internal fun writeSummarySheet(workbook: XSSFWorkbook) {
        // Add summary data
        for ((fileName, result) in fileNameAndResultsList) {
            summaryData.addRow(SummaryRow(fileName = fileName, summary = result.getSummary()))
        }
        summaryData.produceXlsx(workbook, "Summary")
    }

    private fun writeTransductionAnalysisSheet(workbook: XSSFWorkbook) {
        for ((fileName, result) in fileNameAndResultsList) {
            result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cellAnalysis ->
                transductionAnalysisData.addRow(TransductionAnalysisRow(fileName = fileName, transducedCell = i, cellAnalysis = cellAnalysis))
            }
        }
        transductionAnalysisData.produceXlsx(workbook, "Transduction Analysis")
    }

    internal fun writeParamsSheet(workbook: XSSFWorkbook) {
        // Add parameters data
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
        parametersData.produceXlsx(workbook, "Parameters")
    }
}
