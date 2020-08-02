package simplergc.services.colocalizer.output

import java.io.File
import java.io.IOException
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.CsvAggregateGenerator
import simplergc.services.CsvTableWriter
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.batch.output.Metric

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
            "Transduction Efficiency (%)"
        )

        for (metric in Metric.values()) {
            val headerName = metric.summaryName ?: metric.full
            if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                headers.add(headerName)
            } else {
                for (channelName in channelNames) {
                    headers.add("$headerName - $channelName")
                }
            }
        }

        val t = Table()

        t.addRow(FieldRow(headers.map { HeaderField(it) }))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        tableWriter.produce(t, "${outputPath}Summary.csv")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            val t = Table()
            val headers = mutableListOf("File Name",
                "Transduced Cell")

            for (metric in Metric.values()) {
                headers.add(metric.full)
            }

            t.addRow(FieldRow(headers.map { HeaderField(it) }))

            for ((fileName, result) in fileNameAndResultsList) {
                result.channelResults[idx].cellAnalyses.forEachIndexed { i, cellAnalysis ->
                    t.addRow(
                        SingleChannelTransductionAnalysisRow(
                            fileName = fileName,
                            transducedCell = i + 1,
                            cellAnalysis = cellAnalysis
                        )
                    )
                }
                Aggregate.values().forEach {
                    val rawValues = mutableListOf<List<Int>>()
                    Metric.values().forEach { metric ->
                        rawValues.add(result.channelResults[idx].cellAnalyses.map { cell ->
                            metric.compute(cell)
                        })
                    }
                    t.addRow(generateAggregateRow(it, rawValues, spaces = 1))
                }
            }
            tableWriter.produce(t, "${outputPath}Analysis - $name.csv")
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
