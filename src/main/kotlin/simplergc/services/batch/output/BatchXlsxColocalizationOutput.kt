package simplergc.services.batch.output

import java.io.File
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.TableWriter
import simplergc.services.XlsxTableWriter
import simplergc.services.colocalizer.output.XlsxColocalizationOutput

/**
 * Outputs single XLSX file with multiple sheets.
 * Sheets generated are:
 *     - Documentation
 *     - Summary
 *     - Morphology Area
 *     - [metric - channel] for each metric (other than area) and channel
 *     - Parameters
 * For some operations it delegates to colocalizationOutput.
 */
class BatchXlsxColocalizationOutput(outputFile: File, transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    private val workbook = XSSFWorkbook()

    override val colocalizationOutput = XlsxColocalizationOutput(outputFile, transductionParameters, workbook, 2)

    override val tableWriter: TableWriter = XlsxTableWriter(workbook)

    override fun generateAggregateRow(
        aggregate: Aggregate,
        rawValues: List<List<Int>>,
        spaces: Int
    ): AggregateRow {
        return colocalizationOutput.generateAggregateRow(aggregate, rawValues, spaces)
    }

    override fun output() {
        writeSheets()

        colocalizationOutput.writeWorkbook()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun writeMetric(name: String, table: Table) {
        tableWriter.produce(table, name)
    }
}
