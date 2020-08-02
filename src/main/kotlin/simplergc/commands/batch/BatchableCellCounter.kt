package simplergc.commands.batch

import ij.ImagePlus
import org.scijava.Context
import simplergc.commands.RGCCounter
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.services.CellDiameterRange
import simplergc.services.Parameters
import simplergc.services.counter.output.CsvCounterOutput
import simplergc.services.counter.output.XlsxCounterOutput
import java.io.File

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

        val counterParameters = Parameters.Counter(
            targetChannel,
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma
        )

        val output = when (outputFormat) {
            OutputFormat.CSV -> CsvCounterOutput(outputFile, counterParameters)
            OutputFormat.XLSX -> XlsxCounterOutput(outputFile, counterParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        for (image in inputImages) {
            val count = simpleCellCounter.process(image, cellDiameterRange).count
            output.addCountForFile(count, image.title)
        }

        output.output()
    }
}
