package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.Field
import simplergc.services.HeaderField
import simplergc.services.HeaderRow
import simplergc.services.MergedHeaderField
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.XlsxAggregateGenerator
import simplergc.services.XlsxTableWriter

/**
 * Outputs the analysis with the result of overlapping, transduced cells in XLSX format.
 */
class XlsxColocalizationOutput(
    private val outputFile: File,
    transductionParameters: Parameters.Transduction,
    private val workbook: XSSFWorkbook = XSSFWorkbook()
) :
    ColocalizationOutput(transductionParameters) {

    override val tableWriter = XlsxTableWriter(workbook)

    fun writeWorkbook() {
        val filename = FilenameUtils.removeExtension(outputFile.path) ?: "Untitled"
        val file = File("$filename.xlsx")
        val outputStream = file.outputStream()

        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
    }

    override fun output() {
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()
        writeWorkbook()
    }

    override fun summaryData(): Table {
        val channelNames = channelNames()
        val headers = mutableListOf("File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)"
        ).map { HeaderField(it) }

        val metricColumns = listOf("Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluominrescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen").map { MergedHeaderField(HeaderField(it), channelNames.size) }

        val t = Table()

        t.addRow(HeaderRow(headers + metricColumns))

        val subHeaders = MutableList(headers.size) { HeaderField("") }

        for (metricColumn in metricColumns) {
            for (channelName in channelNames) {
                subHeaders.add(HeaderField(channelName))
            }
        }

        t.addRow(HeaderRow(subHeaders))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        return t
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Int>>,
        spaces: Int
    ): AggregateRow {
        var column = 'B' + spaces
        val rowValues = mutableListOf<Field<*>>()
        rawValues.forEach { values ->
            rowValues.add(aggregate.generateValue(
                XlsxAggregateGenerator(column++, values.size)
            ))
        }
        return AggregateRow(aggregate.abbreviation, rowValues, spaces)
    }

    override fun writeSummary() {
        tableWriter.produce(summaryData(), "Summary")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            tableWriter.produce(analysisData(idx), "Analysis - $name")
        }
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "Parameters")
    }
}
