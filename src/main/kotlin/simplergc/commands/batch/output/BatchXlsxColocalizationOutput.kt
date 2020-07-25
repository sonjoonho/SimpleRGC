package simplergc.commands.batch.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.TableProducer
import simplergc.services.XlsxTableProducer
import simplergc.services.colocalizer.output.XlsxColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class BatchXlsxColocalizationOutput(private val transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    private val workbook = XSSFWorkbook()

    override val colocalizationOutput = XlsxColocalizationOutput(transductionParameters, workbook)
    override val tableProducer: TableProducer = XlsxTableProducer(workbook)

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
        tableProducer.produce(documentationData(), "Documentation")
    }

    override fun writeMetricSheet(metric: Metric) {
        tableProducer.produce(metricData(metric), metric.value)
    }
}
