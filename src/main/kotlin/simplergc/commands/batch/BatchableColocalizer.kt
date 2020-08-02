package simplergc.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import org.scijava.Context
import simplergc.commands.ChannelDoesNotExistException
import simplergc.commands.RGCTransduction
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.services.CellDiameterRange
import simplergc.services.Parameters
import simplergc.services.batch.output.BatchCsvColocalizationOutput
import simplergc.services.batch.output.BatchXlsxColocalizationOutput

class BatchableColocalizer(
    private val targetChannel: Int,
    private val shouldRemoveAxonsFromTargetChannel: Boolean,
    private val transducedChannel: Int,
    private val shouldRemoveAxonsFromTransductionChannel: Boolean,
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
        val rgcTransduction = RGCTransduction()

        rgcTransduction.localThresholdRadius = localThresholdRadius
        rgcTransduction.targetChannel = targetChannel
        rgcTransduction.shouldRemoveAxonsFromTargetChannel = shouldRemoveAxonsFromTargetChannel
        rgcTransduction.transducedChannel = transducedChannel
        rgcTransduction.shouldRemoveAxonsFromTransductionChannel = shouldRemoveAxonsFromTransductionChannel
        context.inject(rgcTransduction)

        val transductionParameters = Parameters.Transduction(
            shouldRemoveAxonsFromTargetChannel,
            transducedChannel,
            shouldRemoveAxonsFromTransductionChannel,
            cellDiameterRange.toString(),
            localThresholdRadius,
            gaussianBlurSigma,
            targetChannel
        )

        val output = when (outputFormat) {
            OutputFormat.XLSX -> BatchXlsxColocalizationOutput(outputFile, transductionParameters)
            OutputFormat.CSV -> BatchCsvColocalizationOutput(outputFile, transductionParameters)
            else -> throw IllegalArgumentException("Invalid output type provided: $outputFormat")
        }

        for (image in inputImages) {
            try {
                val analysis = rgcTransduction.process(image, cellDiameterRange)
                output.addTransductionResultForFile(analysis, image.title)
            } catch (e: ChannelDoesNotExistException) {
                MessageDialog(IJ.getInstance(), "Error", e.message)
                return
            }
        }

        output.output()
    }
}
