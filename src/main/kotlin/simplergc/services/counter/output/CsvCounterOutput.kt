package simplergc.services.counter.output

import java.io.File
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.CsvAggregateGenerator
import simplergc.services.CsvTableWriter
import simplergc.services.EmptyRow
import simplergc.services.FieldRow
import simplergc.services.IntField
import simplergc.services.Parameters
import simplergc.services.Table

class CsvCounterOutput(private val outputFile: File, private val counterParameters: Parameters.Counter) : CounterOutput() {

    override val tableWriter = CsvTableWriter()

    /**
     * Saves count results into csv file at specified output path.
     */
    override fun output() {
        val cellCounts = mutableListOf<Int>()
        resultsData.addRow(CitationRow())
        resultsData.addRow(EmptyRow())
        resultsData.addRow(FieldRow(parameterHeadings))
        for ((fileName, count) in fileNameAndCountList) {
            cellCounts.add(count)
            resultsData.addRow(
                ParametersResultsRow(
                    fileName = fileName.replace(",", ""),
                    cellCount = count,
                    morphologyChannel = counterParameters.targetChannel,
                    smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                    largestCellDiameter = counterParameters.cellDiameterRange.largest,
                    localThresholdRadius = counterParameters.localThresholdRadius,
                    gaussianBlurSigma = counterParameters.gaussianBlurSigma
                )
            )
        }
        if (fileNameAndCountList.size > 1) {
            addTotalRow(resultsData, cellCounts)
            Aggregate.values().forEach {
                resultsData.addRow(generateAggregateRow(it, cellCounts))
            }
        }
        tableWriter.produce(resultsData, outputFile.absolutePath)
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        cellCounts: List<Number>,
        spaces: Int,
        startRow: Int
    ): AggregateRow {
        return AggregateRow(
            aggregate.abbreviation,
            listOf(aggregate.generateValue(CsvAggregateGenerator(cellCounts))),
            spaces
        )
    }

    override fun addTotalRow(t: Table, cellCounts: List<Int>) {
        val totalRow = AggregateRow(
            "Total",
            listOf(IntField(cellCounts.sum())),
            spaces = 0
        )
        t.addRow(totalRow)
    }
}
