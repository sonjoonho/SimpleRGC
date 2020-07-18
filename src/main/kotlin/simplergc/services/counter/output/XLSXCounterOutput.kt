package simplergc.services.counter.output

import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.BaseRow
import simplergc.services.DoubleField
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.SimpleOutput.Companion.ARTICLE_CITATION
import simplergc.services.SimpleOutput.Companion.PLUGIN_VERSION
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.counter.output.CounterOutput.Companion.PLUGIN_NAME

class XLSXCounterOutput(private val counterParameters: Parameters.CounterParameters) : CounterOutput {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    private val resultsData = Table(arrayOf("File Name", "Cell Count"))
    data class ResultsRow(val fileName: String, val count: Int) : BaseRow {
        override fun toFieldArray(): Array<Field> = arrayOf(StringField(fileName), IntField(count))
    }
    data class Citation(val article: String = "The article:", val citation: String = ARTICLE_CITATION) : BaseRow {
        override fun toFieldArray(): Array<Field> = arrayOf(StringField(article), StringField(citation))
    }

    /** Add cell count for a filename. */
    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
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

    private val parametersData = Table(arrayOf(
        "File Name",
        "Simple RGC Plugin",
        "Version",
        "Morphology Channel",
        "Smallest Cell Diameter (px)",
        "Largest Cell Diameter (px)",
        "Local Threshold Radius",
        "Gaussian Blur Sigma"
    ))
    data class ParametersRow(
        val fileName: String,
        val pluginName: String = PLUGIN_NAME,
        val pluginVersion: String = PLUGIN_VERSION,
        val targetChannel: Int,
        val smallestCellDiameter: Double,
        val largestCellDiameter: Double,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : BaseRow {
        override fun toFieldArray(): Array<Field> {
            return arrayOf(
                StringField(fileName),
                StringField(pluginName),
                StringField(pluginVersion),
                IntField(targetChannel),
                DoubleField(smallestCellDiameter),
                DoubleField(largestCellDiameter),
                IntField(localThresholdRadius),
                DoubleField(gaussianBlurSigma)
            )
        }
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
