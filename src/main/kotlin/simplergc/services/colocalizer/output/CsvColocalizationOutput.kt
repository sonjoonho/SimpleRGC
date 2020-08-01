package simplergc.services.colocalizer.output

import java.io.File
import java.io.IOException
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.CsvAggregateGenerator
import simplergc.services.CsvTableWriter
import simplergc.services.HeaderField
import simplergc.services.HeaderRow
import simplergc.services.Parameters
import simplergc.services.Table

/**
 * Outputs multiple CSVs into an output folder.
 * CSVs generated are:
 *     - Documentation.csv
 *     - Summary.csv
 *     - Analysis - [Channel].csv for each channel in image
 *     - Parameters.csv
 */
class CsvColocalizationOutput(
    private val outputFile: File,
    transductionParameters: Parameters.Transduction
) :
    ColocalizationOutput(transductionParameters) {

    val outputPath: String = "${outputFile.path}${File.separator}"
    override val tableWriter = CsvTableWriter()

    override fun output() {
        createOutputFolder()
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()
    }

    fun createOutputFolder() {
        val outputFileSuccess = File(outputFile.path).mkdir()
        // If the output file cannot be created, an IOException should be caught
        if (!outputFileSuccess and !outputFile.exists()) {
            throw IOException("Unable to create folder for CSV files.")
        }
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "${outputPath}Documentation.csv")
    }

    override fun writeSummary() {
        val channelNames = channelNames()
        val headers = mutableListOf("File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)",
            "Average Morphology Area (pixel^2)"
        )

        val metricColumns = listOf("Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluominrescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen")

        for (metricColumn in metricColumns) {
            for (channelName in channelNames) {
                headers.add("$metricColumn - $channelName")
            }
        }
        val t = Table()

        t.addRow(HeaderRow(headers.map { HeaderField(it) }))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        tableWriter.produce(t, "${outputPath}Summary.csv")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            tableWriter.produce(analysisData(idx), "${outputPath}Analysis - $name.csv")
        }
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Int>>,
        spaces: Int
    ): AggregateRow {
        return AggregateRow(aggregate.abbreviation, rawValues.map { values ->
            aggregate.generateValue(CsvAggregateGenerator(values))
        }, spaces)
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "${outputPath}Parameters.csv")
    }
}
