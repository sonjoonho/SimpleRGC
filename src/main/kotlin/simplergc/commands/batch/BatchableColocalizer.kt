package simplergc.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import java.io.IOException
import org.scijava.Context
import simplergc.commands.ChannelDoesNotExistException
import simplergc.commands.RGCTransduction
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.displayOutputFileErrorDialog
import simplergc.services.CellDiameterRange
import simplergc.services.colocalizer.output.CSVColocalizationOutput
import simplergc.services.colocalizer.output.XLSXColocalizationOutput

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

        val analyses = inputImages.mapNotNull {
            try {
                rgcTransduction.process(it, cellDiameterRange)
            } catch (e: ChannelDoesNotExistException) {
                MessageDialog(IJ.getInstance(), "Error", e.message)
                null
            }
        }

        val fileNameAndAnalysis = inputImages.map { it.title }.zip(analyses)

        val transductionParameters = RGCTransduction.TransductionParameters(
            shouldRemoveAxonsFromTargetChannel.toString(),
            transducedChannel.toString(),
            shouldRemoveAxonsFromTransductionChannel.toString(),
            cellDiameterRange.toString(),
            localThresholdRadius.toString(),
            gaussianBlurSigma.toString(),
            this.targetChannel.toString(),
            outputFile
        )

        val output = when (outputFormat) {
            OutputFormat.XLSX -> XLSXColocalizationOutput(transductionParameters)
            OutputFormat.CSV -> CSVColocalizationOutput(transductionParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        fileNameAndAnalysis.forEach {
            output.addTransductionResultForFile(it.second, it.first)
        }

        try {
            output.output()
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
        }
    }
}
