package simplergc.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import org.scijava.Context
import simplergc.commands.ChannelDoesNotExistException
import simplergc.commands.RGCTransduction
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.batch.output.BatchCsvColocalizationOutput
import simplergc.commands.batch.output.BatchXlsxColocalizationOutput
import simplergc.services.CellDiameterRange
import simplergc.services.Parameters

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

        val transductionParameters = Parameters.TransductionParameters(
            outputFile,
            shouldRemoveAxonsFromTargetChannel,
            transducedChannel,
            shouldRemoveAxonsFromTransductionChannel,
            cellDiameterRange.toString(),
            localThresholdRadius,
            gaussianBlurSigma,
            targetChannel
        )

        writeOutput(fileNameAndAnalysis, transductionParameters, outputFormat)
    }

    private fun writeOutput(
        fileNameAndAnalysis: List<Pair<String, TransductionResult>>,
        transductionParameters: Parameters.TransductionParameters,
        outputFormat: String
    ) {
        val output = when (outputFormat) {
            OutputFormat.XLSX -> BatchXlsxColocalizationOutput(transductionParameters)
            OutputFormat.CSV -> BatchCsvColocalizationOutput(transductionParameters)
            else -> throw IllegalArgumentException("Invalid output type provided: $outputFormat")
        }

        fileNameAndAnalysis.forEach { output.addTransductionResultForFile(it.second, it.first) }

        output.output()
    }
}
