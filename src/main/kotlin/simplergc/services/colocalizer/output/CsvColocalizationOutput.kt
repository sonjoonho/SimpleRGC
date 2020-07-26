package simplergc.services.colocalizer.output

import java.io.File
import java.io.IOException
import simplergc.services.CsvTableWriter
import simplergc.services.Parameters

/**
 * Outputs multiple CSVs into an output folder.
 * CSVs generated are:
 *     - Documentation.csv
 *     - Summary.csv
 *     - Transduced Cell Analysis.csv
 *     - Parameters.csv
 */
class CsvColocalizationOutput(transductionParameters: Parameters.Transduction) :
    ColocalizationOutput(transductionParameters) {

    val outputPath: String = "${transductionParameters.outputFile.path}${File.separator}"
    override val tableWriter = CsvTableWriter()

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
        tableWriter.produce(documentationData(), "${outputPath}Documentation.csv")
    }

    override fun writeSummary() {
        tableWriter.produce(summaryData(), "${outputPath}Summary.csv")
    }

    override fun writeAnalysis() {
        channelNames().forEachIndexed { idx, name ->
            tableWriter.produce(analysisData(idx), "${outputPath}Analysis - $name.csv")
        }
    }

    override fun writeParameters() {
        tableWriter.produce(parameterData(), "${outputPath}Parameters.csv")
    }
}
