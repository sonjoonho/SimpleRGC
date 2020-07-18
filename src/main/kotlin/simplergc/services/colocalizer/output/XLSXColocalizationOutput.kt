package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XLSXColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) : ColocalizationOutput() {

    companion object {
        const val HEADER_OFFSET = 1
    }

    override fun output() {
        val workbook = XSSFWorkbook()
        writeDocSheet(workbook)

        val headerCellStyle = createHeaderCellStyle(workbook)

        writeSummarySheet(workbook, headerCellStyle)

        writeTransductionAnalysisSheet(workbook, headerCellStyle)

        writeParamsSheet(workbook, headerCellStyle)

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        val docXLSX = Table(arrayOf())
        docXLSX.addRow(DocumentationRow("The article: ", "TODO: Insert citation"))
        docXLSX.addRow(DocumentationRow("", ""))
        docXLSX.addRow(DocumentationRow("Abbreviation", "Description"))
        docXLSX.addRow(DocumentationRow("Summary", "Key measurements per image"))
        docXLSX.addRow(DocumentationRow("Transduced cells analysis", "Per-cell metrics of transduced cells"))
        docXLSX.addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
        docXLSX.produceXLSX(workbook, "Documentation")
    }

    private fun writeSummarySheet(workbook: XSSFWorkbook, headerCellStyle: XSSFCellStyle?) {
        // Add summary data
        fileNameAndResultsList.forEach {
            summaryData.addRow(SummaryRow(it.first, it.second.getSummary()))
        }
        summaryData.produceXLSX(workbook, "Summary")
    }

    private fun writeTransductionAnalysisSheet(workbook: XSSFWorkbook, headerCellStyle: XSSFCellStyle?) {
        fileNameAndResultsList.forEach {
            val fileName = it.first
            it.second.overlappingTransducedIntensityAnalysis.forEach { cellAnalysis ->
                transductionAnalysisData.addRow(TransductionAnalysisRow(fileName, cellAnalysis))
            }
        }
        transductionAnalysisData.produceXLSX(workbook, "Transduction Analysis")
    }

    private fun writeParamsSheet(workbook: XSSFWorkbook, headerCellStyle: XSSFCellStyle?) {
        // Add parameters data
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
        parametersData.produceXLSX(workbook, "Parameters")
    }

    private fun createHeaderCellStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.BLUE.getIndex()

        val headerCellStyle = workbook.createCellStyle()
        headerCellStyle.setFont(headerFont)
        return headerCellStyle
    }
}
