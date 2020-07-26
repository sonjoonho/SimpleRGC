package simplergc.services.batch.output

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.AggregateRow
import simplergc.services.Field
import simplergc.services.FormulaField
import simplergc.services.Parameters
import simplergc.services.TableWriter
import simplergc.services.XlsxTableWriter
import simplergc.services.colocalizer.output.XlsxColocalizationOutput

class XlsxAggregateGenerator(column: Char, numCells: Int) : AggregateGenerator() {

    private val startCellRow = 2
    private val endCellRow = numCells + startCellRow - 1
    private val cellRange = "$column$startCellRow:$column$endCellRow"

    override fun generateMean(): Field<*> {
        return FormulaField("AVERAGE($cellRange)")
    }

    override fun generateStandardDeviation(): Field<*> {
        return FormulaField("STDEV($cellRange)")
    }

    override fun generateStandardErrorOfMean(): Field<*> {
        return FormulaField("STDEV($cellRange)/SQRT(COUNT($cellRange))")
    }

    override fun generateCount(): Field<*> {
        return FormulaField("COUNT($cellRange)")
    }
}

/**
 * Outputs single XLSX file with multiple sheets.
 * Sheets generated are:
 *     - Documentation
 *     - Summary
 *     - [metric].csv for each metric
 *     - Parameters
 * For some operations it delegates to colocalizationOutput.
 */
class BatchXlsxColocalizationOutput(transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    private val workbook = XSSFWorkbook()

    override val colocalizationOutput = XlsxColocalizationOutput(transductionParameters, workbook)

    override val tableWriter: TableWriter = XlsxTableWriter(workbook)

    override fun generateAggregateRow(
        aggregate: Aggregate,
        fileValues: List<List<Int>>
    ): AggregateRow {
        var column = 'B'
        val rowValues = mutableListOf<Field<*>>()
        fileValues.forEach { values ->
            rowValues.add(aggregate.generateValue(XlsxAggregateGenerator(
                column++, values.size
            )))
        }
        return AggregateRow(aggregate.abbreviation, rowValues)
    }

    override fun output() {
        writeSheets()

        colocalizationOutput.writeWorkbook()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun writeMetric(metric: Metric) {
        for (metricTable in metricData(metric)) {
            tableWriter.produce(metricTable.second, metricTable.first)
        }
    }
}
