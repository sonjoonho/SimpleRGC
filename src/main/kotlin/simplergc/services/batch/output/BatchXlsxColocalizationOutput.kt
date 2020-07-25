package simplergc.services.batch.output

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.TableWriter
import simplergc.services.XlsxTableWriter
import simplergc.services.colocalizer.output.XlsxColocalizationOutput

/**
 * Outputs single XLSX file with multiple sheets.
 * Sheets generated are:
 *     - Documentation
 *     - [metric].csv for each metric
 *
 * For some operations it delegates to colocalizationOutput.
 */
class BatchXlsxColocalizationOutput(transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    private val workbook = XSSFWorkbook()

    override val colocalizationOutput = XlsxColocalizationOutput(transductionParameters, workbook)
    override val tableWriter: TableWriter = XlsxTableWriter(workbook)

    override fun output() {
        writeSheets()

        colocalizationOutput.writeWorkbook()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun writeMetrics(metric: Metric) {
        tableWriter.produce(metricData(metric), metric.value)
    }
}
