package simplergc.services.batch.output

import java.io.File
import org.apache.commons.io.FilenameUtils
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
 */
class BatchXlsxColocalizationOutput(private val transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    private val workbook = XSSFWorkbook()

    override val colocalizationOutput = XlsxColocalizationOutput(transductionParameters, workbook)
    override val tableWriter: TableWriter = XlsxTableWriter(workbook)

    override fun output() {
        writeDocumentation()
        colocalizationOutput.writeSummary()

        for (metric in Metric.values()) {
            writeMetricSheet(metric)
        }

        colocalizationOutput.writeParameters()

        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun writeMetricSheet(metric: Metric) {
        tableWriter.produce(metricData(metric), metric.value)
    }
}
