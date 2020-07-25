package simplergc.services.colocalizer.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table
import java.io.File

/**
 * Outputs the analysis with the result of overlapping, transduced cells in XLSX format.
 */
class XlsxColocalizationOutput(
    private val transductionParameters: Parameters.TransductionParameters,
    val workbook: XSSFWorkbook = XSSFWorkbook()
) :
    ColocalizationOutput() {

    override fun output() {
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()

        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    override fun writeDocumentation() {
        val docXlsx = Table(listOf())
        docXlsx.addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
        docXlsx.addRow(DocumentationRow("", ""))
        docXlsx.addRow(DocumentationRow("Abbreviation", "Description"))
        docXlsx.addRow(DocumentationRow("Summary", "Key measurements per image"))
        docXlsx.addRow(DocumentationRow("Transduced cells analysis", "Per-cell metrics of transduced cells"))
        docXlsx.addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
        docXlsx.produceXlsx(workbook, "Documentation")
    }

    override fun writeSummary() {
        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            summaryData.addRow(SummaryRow(fileName = fileName, summary = result.getSummary()))
        }
        summaryData.produceXlsx(workbook, "Summary")
    }

    override fun writeAnalysis() {
        for ((fileName, result) in fileNameAndResultsList) {
            result.overlappingTransducedIntensityAnalysis.forEachIndexed { i, cellAnalysis ->
                transductionAnalysisData.addRow(
                    TransductionAnalysisRow(
                        fileName = fileName,
                        transducedCell = i,
                        cellAnalysis = cellAnalysis
                    )
                )
            }
        }
        transductionAnalysisData.produceXlsx(workbook, "Transduction Analysis")
    }

    override fun writeParameters() {
        // Add parameter data.
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
