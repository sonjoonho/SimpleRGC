package simplergc.services.colocalizer.output

import simplergc.services.CsvTableProducer
import simplergc.services.Parameters
import java.io.File
import java.io.IOException

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CsvColocalizationOutput(transductionParameters: Parameters.Transduction) :
    ColocalizationOutput(transductionParameters) {

    val outputPath: String = "${transductionParameters.outputFile.path}${File.separator}"
    override val tableProducer = CsvTableProducer()

    override fun output() {
        createOutputFolder()
        writeDocumentation()
        writeSummary()
        writeAnalysis()
        writeParameters()
    }

    fun createOutputFolder() {
        val outputFileSuccess = File(transductionParameters.outputFile.path).mkdir()
        // If the output file cannot be created, an IOException should be caught
        if (!outputFileSuccess and !transductionParameters.outputFile.exists()) {
            throw IOException("Unable to create folder for CSV files.")
        }
    }

    override fun writeDocumentation() {
        tableProducer.produce(documentationData(), "${outputPath}Documentation.csv")
    }

    override fun writeSummary() {
        tableProducer.produce(summaryData(), "${outputPath}Summary.csv")
    }

    override fun writeAnalysis() {
        tableProducer.produce(analysisData(), "${outputPath}Transduced Cell Analysis.csv")
    }

    override fun writeParameters() {
        tableProducer.produce(parameterData(), "${outputPath}Parameters.csv")
    }
}
