package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.Field
import simplergc.services.HeaderField
import simplergc.services.FieldRow
import simplergc.services.HorizontallyMergedHeaderField
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.StringField
import simplergc.services.Table
import simplergc.services.VerticallyMergedHeaderField
import simplergc.services.XlsxAggregateGenerator
import simplergc.services.XlsxTableWriter
import simplergc.services.batch.output.Metric

/**
 * Outputs the analysis with the result of overlapping, transduced cells in XLSX format.
 */
class XlsxColocalizationOutput(
    private val outputFile: File,
    transductionParameters: Parameters.Transduction,
    private val workbook: XSSFWorkbook = XSSFWorkbook(),
    val cellStartRow: Int = 3
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
                XlsxAggregateGenerator(cellStartRow, column++, values.size)
            ))
        }
        return AggregateRow(aggregate.abbreviation, rowValues, spaces)
    }

    override fun writeSummary() {
        val channelNames = channelNames()
        val headers = mutableListOf("File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)"
        ).map { VerticallyMergedHeaderField(HeaderField(it), 2) }

        val metricColumns = listOf("Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen").map { HorizontallyMergedHeaderField(HeaderField(it), channelNames.size) }

        val t = Table()

        t.addRow(FieldRow(headers + metricColumns))

        val subHeaders: MutableList<Field<*>> = MutableList(headers.size) { StringField("") }

        for (metricColumn in metricColumns) {
            for (channelName in channelNames) {
                subHeaders.add(HeaderField(channelName))
            }
        }

        t.addRow(FieldRow(subHeaders))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        tableWriter.produce(t, "Summary")
    }

    override fun writeAnalysis() {
        val t = Table()
        val channelNames = channelNames()
        val headers = listOf(
            "File Name",
            "Transduced Cell",
            "Morphology Area (pixel^2)").map { VerticallyMergedHeaderField(HeaderField(it), 2) }

        val metricColumns = listOf(
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen"
        ).map { HorizontallyMergedHeaderField(HeaderField(it), channelNames.size) }

        t.addRow(FieldRow(headers + metricColumns))

        val subHeaders: MutableList<Field<*>> = MutableList(headers.size) { StringField("") }

        for (metricColumn in metricColumns) {
            for (channelName in channelNames) {
                subHeaders.add(HeaderField(channelName))
            }
        }

        t.addRow(FieldRow(subHeaders))

        for ((fileName, result) in fileNameAndResultsList) {
            result.channelResults[transducedChannel].cellAnalyses.forEachIndexed { i, cellAnalysis ->
                val row: MutableList<Field<*>> = mutableListOf(StringField(fileName),
                    IntField(i + 1)
                )
                for (metric in Metric.values()) {
                    if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                        row.add(IntField(metric.compute(cellAnalysis)))
                    } else {
                        for (channel in channelNames.withIndex()) {
                            row.add(IntField(metric.compute(result.channelResults[channel.index].cellAnalyses[i])))
                        }
                    }
                }
                t.addRow(FieldRow(row))
            }

            Aggregate.values().forEach {
                val rawValues = mutableListOf<List<Int>>()
                Metric.values().forEach { metric ->
                    if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                        rawValues.add(result.channelResults[transducedChannel].cellAnalyses.map { cell ->
                            metric.compute(cell)
                        })
                    } else {
                        channelNames.indices.forEach { idx ->
                            rawValues.add(result.channelResults[idx].cellAnalyses.map { cell ->
                                metric.compute(cell)
                            })
                        }
                    }
                }
                t.addRow(generateAggregateRow(it, rawValues, spaces = 1))
            }
        }
        tableWriter.produce(t, "Transduced cell analysis")
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "Parameters")
    }
}
