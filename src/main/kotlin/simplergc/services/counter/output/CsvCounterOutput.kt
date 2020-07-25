package simplergc.services.counter.output

import simplergc.services.CsvTableProducer
import simplergc.services.Parameters

class CsvCounterOutput(private val counterParameters: Parameters.Counter) : CounterOutput() {

    override val tableProducer = CsvTableProducer()

    /**
     * Saves count results into csv file at specified output path.
     */
    override fun output() {
        for ((fileName, count) in fileNameAndCountList) {
            parametersAndResultsData.addRow(
                ParametersResultsRow(
                    fileName = fileName.replace(",", ""),
                    cellCount = count,
                    morphologyChannel = counterParameters.targetChannel,
                smallestCellDiameter = counterParameters.cellDiameterRange.smallest,
                largestCellDiameter = counterParameters.cellDiameterRange.largest,
                localThresholdRadius = counterParameters.localThresholdRadius,
                gaussianBlurSigma = counterParameters.gaussianBlurSigma
                ))
        }
        tableProducer.produce(parametersAndResultsData, counterParameters.outputFile.absolutePath)
    }
}
