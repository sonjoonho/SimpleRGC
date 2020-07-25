package simplergc.services.counter.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.BaseRow
import simplergc.services.Parameters
import simplergc.services.SimpleOutput.Companion.ARTICLE_CITATION
import simplergc.services.StringField
import java.io.File

class XlsxCounterOutput(private val counterParameters: Parameters.Counter) : CounterOutput() {

    data class Citation(val article: String = "The article:", val citation: String = ARTICLE_CITATION) : BaseRow {
        override fun toList() = listOf(StringField(article), StringField(citation))
    }

    /**
     * Generate the 'Results' sheet, containing the cell counts for each filename.
     */
    private fun generateResultsSheet(workbook: XSSFWorkbook) {
        val createHelper = workbook.creationHelper
        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")
        for ((fileName, count) in fileNameAndCountList) {
            resultsData.addRow(ResultsRow(fileName.replace(",", ""), count))
        }
        resultsData.addRow(Citation())
        resultsData.produceXlsx(workbook, "Results")
    }

    /**
     * Generate the 'Parameters' sheet, containing the parameters used for each filename.
     */
    private fun generateParametersSheet(workbook: XSSFWorkbook) {
        for ((fileName, _) in fileNameAndCountList) {
            parametersData.addRow(ParametersRow(
                fileName = fileName.replace(",", ""),
                targetChannel = counterParameters.targetChannel,
                smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                largestCellDiameter = counterParameters.cellDiameterRange.largest,
                localThresholdRadius = counterParameters.localThresholdRadius,
                gaussianBlurSigma = counterParameters.gaussianBlurSigma
            ))
        }
        parametersData.produceXlsx(workbook, "Parameters")
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        generateResultsSheet(workbook)
        generateParametersSheet(workbook)

        val outputXlsxFile = File(FilenameUtils.removeExtension(counterParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }
}
