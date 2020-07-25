package simplergc.commands.batch.output

import simplergc.services.CsvTableProducer
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CsvColocalizationOutput

/**
 * Outputs multiple CSVs into an output folder.
 * CSVs generated are:
 *     - Documentation.csv
 *     - Summary.csv
 *     - [metric].csv for each metric
 *     - Parameters.csv
 */
class BatchCsvColocalizationOutput(transductionParameters: Parameters.Transduction) :
    BatchColocalizationOutput() {

    override val colocalizationOutput = CsvColocalizationOutput(transductionParameters)
    override val tableProducer = CsvTableProducer()

    override fun output() {
        colocalizationOutput.createOutputFolder()

        writeDocumentation()
        colocalizationOutput.writeSummary()
        for (metric in Metric.values()) {
            writeMetricSheet(metric)
        }
        colocalizationOutput.writeParameters()
    }

    override fun writeDocumentation() {
        tableProducer.produce(documentationData(), "${colocalizationOutput.outputPath}Documentation.csv")
    }

    override fun writeMetricSheet(metric: Metric) {
        tableProducer.produce(metricData(metric), "${colocalizationOutput.outputPath}${metric.value}.csv")
    }
}
