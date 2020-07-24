package simplergc.commands.batch

import ij.ImagePlus
import java.io.File
import java.io.IOException
import org.scijava.Context
import simplergc.commands.RGCCounter
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.displayOutputFileErrorDialog
import simplergc.services.CellDiameterRange
import simplergc.services.Parameters
import simplergc.services.counter.output.CSVCounterOutput
import simplergc.services.counter.output.XLSXCounterOutput

class BatchableCellCounter(
    private val targetChannel: Int,
    private val shouldRemoveAxons: Boolean,
    private val context: Context
) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        cellDiameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    ) {
        val simpleCellCounter = RGCCounter()

        simpleCellCounter.targetChannel = targetChannel
        simpleCellCounter.localThresholdRadius = localThresholdRadius
        simpleCellCounter.gaussianBlurSigma = gaussianBlurSigma
        simpleCellCounter.shouldRemoveAxons = shouldRemoveAxons
        context.inject(simpleCellCounter)

        val numCellsList = inputImages.map { simpleCellCounter.process(it, cellDiameterRange).count }
        val imageAndCount = inputImages.zip(numCellsList)

        val counterParameters = Parameters.CounterParameters(
            outputFile,
            targetChannel,
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma
        )

        val output = when (outputFormat) {
            OutputFormat.CSV -> CSVCounterOutput(counterParameters)
            OutputFormat.XLSX -> XLSXCounterOutput(counterParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        imageAndCount.forEach { output.addCountForFile(it.second, it.first.title) }
        try {
            output.output()
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
        }
    }
}
