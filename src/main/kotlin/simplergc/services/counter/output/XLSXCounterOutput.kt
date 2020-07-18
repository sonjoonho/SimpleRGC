package simplergc.services.counter.output

import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.BaseRow
import simplergc.services.Field
import simplergc.services.Parameters
import simplergc.services.SimpleOutput.Companion.ARTICLE_CITATION
import simplergc.services.StringField

class XLSXCounterOutput(private val counterParameters: Parameters.CounterParameters) : CounterOutput() {

    data class Citation(val article: String = "The article:", val citation: String = ARTICLE_CITATION) : BaseRow {
        override fun toFieldArray(): Array<Field> = arrayOf(StringField(article), StringField(citation))
    }

    /**
     * Generate the 'Results' sheet, containing the cell counts for each filename.
     */
    private fun generateResultsSheet(workbook: XSSFWorkbook) {
        val createHelper = workbook.creationHelper
        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")
        for (pair in fileNameAndCountList) {
            resultsData.addRow(ResultsRow(pair.first.replace(",", ""), pair.second))
        }
        resultsData.addRow(Citation())
        resultsData.produceXLSX(workbook, "Results")
    }

    /**
     * Generate the 'Parameters' sheet, containing the parameters used for each filename.
     */
    private fun generateParametersSheet(workbook: XSSFWorkbook) {
        for (pair in fileNameAndCountList) {
            parametersData.addRow(ParametersRow(
                fileName = pair.first.replace(",", ""),
                targetChannel = counterParameters.targetChannel,
                smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                largestCellDiameter = counterParameters.cellDiameterRange.largest,
                localThresholdRadius = counterParameters.localThresholdRadius,
                gaussianBlurSigma = counterParameters.gaussianBlurSigma
            ))
        }
        parametersData.produceXLSX(workbook, "Parameters")
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        val workbook = XSSFWorkbook()

        generateResultsSheet(workbook)
        generateParametersSheet(workbook)

        val fileOut = FileOutputStream(counterParameters.outputFile)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    }
}
