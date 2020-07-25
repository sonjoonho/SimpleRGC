package simplergc.commands.batch.output

import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.Table
import simplergc.services.colocalizer.output.XlsxColocalizationOutput
import java.io.File

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXlsxColocalizationOutput(private val transductionParameters: Parameters.Transduction) :
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
        print(outputXlsxFile.absoluteFile)
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
