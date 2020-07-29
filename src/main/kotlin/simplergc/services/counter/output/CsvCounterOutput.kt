package simplergc.services.counter.output

import java.io.File
import simplergc.services.CsvTableWriter
import simplergc.services.Parameters

class CsvCounterOutput(private val outputFile: File, private val counterParameters: Parameters.Counter) : CounterOutput() {

    override val tableWriter = CsvTableWriter()

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
        tableWriter.produce(parametersAndResultsData, outputFile.absolutePath)
    }
}
