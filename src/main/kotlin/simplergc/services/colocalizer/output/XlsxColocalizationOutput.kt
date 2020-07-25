package simplergc.services.colocalizer.output

import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import simplergc.services.Parameters
import simplergc.services.XlsxTableProducer

/**
 * Outputs the analysis with the result of overlapping, transduced cells in XLSX format.
 */
class XlsxColocalizationOutput(
    transductionParameters: Parameters.Transduction,
    workbook: XSSFWorkbook = XSSFWorkbook()
) :
    ColocalizationOutput(transductionParameters) {

    private val workbook = XSSFWorkbook()
    override val tableProducer = XlsxTableProducer(workbook)

    override fun output() {
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()

        val outputXlsxFile = File(FilenameUtils.removeExtension(transductionParameters.outputFile.path) + ".xlsx")
        val xlsxFileOut = outputXlsxFile.outputStream()
        workbook.write(xlsxFileOut)
        xlsxFileOut.close()
        workbook.close()
    }

    override fun writeDocumentation() {
        tableProducer.produce(documentationData(), "Documentation")
    }

    override fun writeSummary() {
        tableProducer.produce(summaryData(), "Summary")
    }

    override fun writeAnalysis() {
        tableProducer.produce(analysisData(), "Transudction Analysis")
    }

    override fun writeParameters() {
        tableProducer.produce(parameterData(), "Parameters")
    }
}
