package simplergc.services.batch.output

import simplergc.services.CsvTableWriter
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CsvColocalizationOutput

/**
 * Outputs multiple CSVs into an output folder.
 * CSVs generated are:
 *     - Documentation.csv
 *     - Summary.csv
 *     - [metric].csv for each metric
 *     - Parameters.csv
 *
 * For some operations it delegates to colocalizationOutput.
 */
class BatchCsvColocalizationOutput(transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    override val colocalizationOutput = CsvColocalizationOutput(transductionParameters)
    override val tableWriter = CsvTableWriter()

    override fun output() {
        colocalizationOutput.createOutputFolder()

        writeSheets()

        colocalizationOutput.writeParameters()
    }

    override fun writeDocumentation() {
        tableWriter.produce(documentationData(), "${colocalizationOutput.outputPath}Documentation.csv")
    }

    override fun writeMetrics(metric: Metric) {
        tableWriter.produce(metricData(metric), "${colocalizationOutput.outputPath}${metric.value}.csv")
    }
}
