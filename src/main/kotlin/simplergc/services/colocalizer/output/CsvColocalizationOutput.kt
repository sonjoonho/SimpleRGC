package simplergc.services.colocalizer.output

import java.io.File
import java.io.IOException
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.CsvAggregateGenerator
import simplergc.services.CsvTableWriter
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.Metric
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

    override fun writeSummaryWithAggregates() {
        val t = getSummaryTable()
        val rawValues = getSummaryRawValues()

        // rawValues[0] and rawValues[1] contain cell counts, and are guaranteed to contain integers.
        @Suppress("UNCHECKED_CAST")
        val rawCellCounts = rawValues[0] as List<Int>
        @Suppress("UNCHECKED_CAST")
        val rawTransducedCellCounts = rawValues[1] as List<Int>

        addTotalRow(t, rawCellCounts = rawCellCounts, rawTransducedCellCounts = rawTransducedCellCounts)

        Aggregate.values().forEach {
            t.addRow(generateAggregateRow(it, rawValues, spaces = 0))
        }

        tableWriter.produce(t, "${outputPath}Summary.csv")
    }

    override fun getSummaryTable(): Table {
        val channelNames = channelNames()
        val headers = mutableListOf(
            "File Name",
            "Number of Cells",
            "Number of Transduced Cells",
            "Transduction Efficiency (%)"
        )

        for (metric in Metric.values()) {
            if (metric.channels == Metric.ChannelSelection.TRANSDUCTION_ONLY) {
                headers.add(metric.summaryHeader)
            } else {
                for (channelName in channelNames) {
                    headers.add("${metric.summaryHeader} - $channelName")
                }
            }
        }

        val t = Table()

        t.addRow(FieldRow(headers.map { HeaderField(it) }))

        // Add summary data.
        for ((fileName, result) in fileNameAndResultsList) {
            t.addRow(SummaryRow(fileName = fileName, summary = result))
        }
        return t
    }

    override fun writeSummary() {
        val t = getSummaryTable()
        tableWriter.produce(t, "${outputPath}Summary.csv")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            val t = Table()
            val headers = mutableListOf(
                "File Name",
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
                    val rawValues = mutableListOf<List<Number>>()
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

    /**
     * [startRow] is unused for the CSV output.
     */
    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Number>>,
        spaces: Int,
        startRow: Int
    ): AggregateRow {
        val aggregates = rawValues.map { values ->
            aggregate.generateValue(CsvAggregateGenerator(values))
        }

        return AggregateRow(aggregate.abbreviation, aggregates, spaces)
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "${outputPath}Parameters.csv")
    }
}
