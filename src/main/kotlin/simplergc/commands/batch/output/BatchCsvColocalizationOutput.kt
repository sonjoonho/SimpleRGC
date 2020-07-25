package simplergc.commands.batch.output

import simplergc.services.CsvTableProducer
import simplergc.services.Parameters
import simplergc.services.colocalizer.output.CsvColocalizationOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
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
