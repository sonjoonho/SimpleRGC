package simplergc.commands.batch

import de.siegmar.fastcsv.writer.CsvWriter
import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.scijava.Context
import simplergc.commands.ChannelDoesNotExistException
import simplergc.commands.RGCTransduction
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.displayOutputFileErrorDialog
import simplergc.services.CellDiameterRange

class BatchableColocalizer(
    private val targetChannel: Int,
    private val transducedChannel: Int,
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
        rgcTransduction.transducedChannel = transducedChannel
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

        try {
            when (outputFormat) {
                OutputFormat.CSV -> outputToCSV(fileNameAndAnalysis, outputFile)
                else -> throw IllegalArgumentException("Invalid output type provided")
            }
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
        }
    }

    private fun outputToCSV(
        fileNameAndAnalysis: List<Pair<String, TransductionResult>>,
        outputFile: File
    ) {
        val csvWriter = CsvWriter()
        val outputData = mutableListOf(
            arrayOf(
                "File Name",
                "Total number of cells in cell morphology channel 1",
                "Transduced cells in channel 1",
                "Transduced cells in both morphology channels"
            )
        )
        outputData.addAll(fileNameAndAnalysis.map {
            val totalTargetCells = it.second.targetCellCount.toString()
            val totalTransducedTargetCells = it.second.overlappingTwoChannelCells.size.toString()
            arrayOf(it.first.replace(",", ""), totalTargetCells, totalTransducedTargetCells)
        })
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }

}
