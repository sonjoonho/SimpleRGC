package simplergc.services.batch.output

import kotlin.math.pow
import kotlin.math.sqrt
import simplergc.services.AggregateRow
import simplergc.services.CsvTableWriter
import simplergc.services.DoubleField
import simplergc.services.Field
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.CsvColocalizationOutput

class CsvAggregateGenerator(val values: List<Int>) : AggregateGenerator() {

    private fun computeStandardDeviation(): Double {
        val squareOfMean = values.average().pow(2)
        val meanOfSquares = values.map { it.toDouble().pow(2) }.average()
        return sqrt(meanOfSquares - squareOfMean)
    }

    override fun generateMean(): Field<*> {
        return DoubleField(values.average())
    }

    override fun generateStandardDeviation(): Field<*> {
        return DoubleField(computeStandardDeviation())
    }

    override fun generateStandardErrorOfMean(): Field<*> {
        return DoubleField(computeStandardDeviation() / sqrt(values.size.toDouble()))
    }

    override fun generateCount(): Field<*> {
        return IntField(values.size)
    }
}

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
        fileValues: List<List<Int>>
    ): AggregateRow {
        return AggregateRow(aggregate.abbreviation, fileValues.map { values ->
            aggregate.generateValue(CsvAggregateGenerator(values))
        })
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
