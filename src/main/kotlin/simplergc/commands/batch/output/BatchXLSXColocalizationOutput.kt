package simplergc.commands.batch.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.XLSXColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXLSXColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput() {

    private val XLSXColocalizationOutput = XLSXColocalizationOutput(transductionParameters)

    override fun output() {
        val workbook = XSSFWorkbook()
        writeDocSheet(workbook)
        XLSXColocalizationOutput.writeSummarySheet(workbook)
        for (metricName in metricMappings.keys) {
            writeMetricSheet(metricName, workbook)
        }
        XLSXColocalizationOutput.writeParamsSheet(workbook)

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
        docXLSX.addRow(DocumentationRow("Mean Int: ", "Mean fluorescence intensity for each transduced cell"))
        docXLSX.addRow(DocumentationRow("Median Int:", "Median fluorescence intensity for each transduced cell"))
        docXLSX.addRow(DocumentationRow("Min Int: ", "Min fluorescence intensity for each transduced cell"))
        docXLSX.addRow(DocumentationRow("Max Int: ", "Max fluorescence intensity for each transduced cell"))
        docXLSX.addRow(DocumentationRow("Raw IntDen:", "Raw Integrated Density for each transduced cell"))
        docXLSX.addRow(DocumentationRow("Parameters", "Parameters used to run the SimpleRGC plugin"))
        docXLSX.produceXLSX(workbook, "Documentation")
    }

    private fun writeMetricSheet(metricName: String, workbook: XSSFWorkbook) {
        val maxRows =
            fileNameAndResultsList.maxBy { it.second.overlappingTwoChannelCells.size }?.second?.overlappingTwoChannelCells?.size
        for (rowIdx in 0..maxRows!!) {
            val rowData = metricMappings.getOrDefault(metricName, emptyList()).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(metricRow(rowIdx, rowData))
        }
        metricData.produceXLSX(workbook, metricName)
    }
}
