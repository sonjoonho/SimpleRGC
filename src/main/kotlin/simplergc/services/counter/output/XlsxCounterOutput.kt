package simplergc.services.counter.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.BaseRow
import simplergc.services.EmptyRow
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.Output.Companion.ARTICLE_CITATION
import simplergc.services.Parameters
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.XlsxAggregateGenerator
import simplergc.services.XlsxTableWriter

data class CitationRow(val article: String = "The article:", val citation: String = ARTICLE_CITATION) : BaseRow {
    override fun toList() = listOf(StringField(article), StringField(citation))
}

class XlsxCounterOutput(private val outputFile: File, private val counterParameters: Parameters.Counter) : CounterOutput() {

    private val workbook = XSSFWorkbook()

    override val tableWriter = XlsxTableWriter(workbook)

    /**
     * Generate the 'Results' sheet, containing the cell counts for each filename.
     */
    private fun writeResults() {
        val cellCounts = mutableListOf<Int>()
        val createHelper = workbook.creationHelper
        val numberCellStyle = workbook.createCellStyle()
        numberCellStyle.dataFormat = createHelper.createDataFormat().getFormat("#")
        resultsData.addRow(CitationRow())
        resultsData.addRow(EmptyRow())
        resultsData.addRow(FieldRow(listOf("File Name", "Cell Count").map { HeaderField(it) }))

        for ((fileName, count) in fileNameAndCountList) {
            cellCounts.add(count)
            resultsData.addRow(ResultsRow(fileName.replace(",", ""), count))
        }
        addTotalRow(resultsData, cellCounts)
        Aggregate.values().forEach {
            resultsData.addRow(generateAggregateRow(it, cellCounts))
        }
        tableWriter.produce(resultsData, "Results")
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        cellCounts: List<Number>,
        spaces: Int,
        startRow: Int
    ): AggregateRow {
        val column = 'B' + spaces
        val rowValues = mutableListOf(
            aggregate.generateValue(
                XlsxAggregateGenerator(startRow, column, cellCounts.size)
            )
        )
        return AggregateRow(aggregate.abbreviation, rowValues, spaces)
    }

    /**
     * Generate the 'Parameters' sheet, containing the parameters used for each filename.
     */
    private fun writeParameters() {
        val parametersData = Table()
        parametersData.addRow(FieldRow(parameterHeadings))
        for ((fileName, _) in fileNameAndCountList) {
            parametersData.addRow(
                ParametersRow(
                    fileName = fileName.replace(",", ""),
                    targetChannel = counterParameters.targetChannel,
                    smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                    largestCellDiameter = counterParameters.cellDiameterRange.largest,
                    localThresholdRadius = counterParameters.localThresholdRadius,
                    gaussianBlurSigma = counterParameters.gaussianBlurSigma
                )
            )
        }
        tableWriter.produce(parametersData, "Parameters")
    }

    /**
     * Saves count results into excel file at specified output path.
     */
    override fun output() {
        writeResults()
        writeParameters()

        val filename = FilenameUtils.removeExtension(outputFile.path) ?: "Untitled"
        val file = File("$filename.xlsx")
        val outputStream = file.outputStream()

        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
    }
}
