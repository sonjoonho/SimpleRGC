package simplergc.services.batch.output

import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.CsvTableWriter
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.CsvColocalizationOutput

/**
 * Outputs multiple CSVs into an output folder.
 * CSVs generated are:
 *     - Documentation.csv
 *     - Summary.csv
 *     - Morphology Area.csv
 *     - [metric - channel].csv for each metric (other than area) and channel
 *     - Parameters.csv
 *
 * For some operations it delegates to colocalizationOutput.
 */
class BatchCsvColocalizationOutput(transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    override val colocalizationOutput = CsvColocalizationOutput(transductionParameters)

    override val tableWriter = CsvTableWriter()

    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Int>>,
        spaces: Int
    ): AggregateRow {
        return colocalizationOutput.generateAggregateRow(aggregate, rawValues, spaces)
    }

    override fun output() {
        colocalizationOutput.createOutputFolder()

        writeSheets()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "${colocalizationOutput.outputPath}Documentation.csv")
    }

    override fun writeMetric(name: String, table: Table) {
        tableWriter.produce(table, "${colocalizationOutput.outputPath}$name.csv")
    }
}
