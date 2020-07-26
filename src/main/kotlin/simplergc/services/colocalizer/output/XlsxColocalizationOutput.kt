package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.XlsxTableWriter

/**
 * Outputs the analysis with the result of overlapping, transduced cells in XLSX format.
 */
class XlsxColocalizationOutput(
    transductionParameters: Parameters.Transduction,
    private val workbook: XSSFWorkbook = XSSFWorkbook()
) :
    ColocalizationOutput(transductionParameters) {

    override val tableWriter = XlsxTableWriter(workbook)

    fun writeWorkbook() {
        val filename = FilenameUtils.removeExtension(transductionParameters.outputFile.path) ?: "Untitled"
        val file = File("$filename.xlsx")
        val outputStream = file.outputStream()

        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
    }

    override fun output() {
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()
        writeWorkbook()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "Documentation")
    }

    override fun writeSummary() {
        tableWriter.produce(summaryData(), "Summary")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            tableWriter.produce(analysisData(idx), "Analysis - $name")
        }
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "Parameters")
    }
}
