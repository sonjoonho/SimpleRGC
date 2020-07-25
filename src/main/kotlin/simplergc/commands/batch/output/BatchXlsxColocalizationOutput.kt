package simplergc.commands.batch.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.XlsxColocalizationOutput

/**
 * Outputs single XLSX file with multiple sheets.
 * Sheets generated are:
 *     - Documentation
 *     - [metric].csv for each metric
 */
class BatchXlsxColocalizationOutput(private val transductionParameters: Parameters.TransductionParameters) :
    BatchColocalizationOutput(XlsxColocalizationOutput(transductionParameters)) {

    private val workbook = XSSFWorkbook()

    override fun output() {
        writeDocSheet(workbook)
        colocalizatonOutput.writeSummary()
        for (metric in Metric.values()) {
            writeMetricSheet(metric)
        }
        colocalizatonOutput.writeParameters()

        // Write file and close streams
        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    private fun writeDocSheet(workbook: XSSFWorkbook) {
        val docXlsx = Table(emptyList())
        for (row in documentationRows) {
            docXlsx.addRow(row)
        }
        docXlsx.produceXlsx(workbook, "Documentation")
    }

    override fun writeMetricSheet(metric: Metric) {
        val maxRows = maxRows()
        val metricData = metricData()
        for (rowIdx in 0..maxRows) {
            val rowData = metricMappings().getValue(metric).map { it.second.getOrNull(rowIdx) }
            metricData.addRow(MetricRow(rowIdx, rowData))
        }
        metricData.produceXlsx(workbook, metric.value)
    }
}
