package simplecolocalization.commands.batch

import ij.ImagePlus
import java.io.File
import java.io.IOException
import javax.xml.transform.TransformerException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.commands.displayOutputFileErrorDialog
import simplecolocalization.services.CellDiameterRange
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput

class BatchableCellCounter(private val targetChannel: Int, private val context: Context) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        cellDiameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        shouldRemoveAxons: Boolean,
        outputFormat: String,
        outputFile: File
    ) {
        val simpleCellCounter = SimpleCellCounter()

        simpleCellCounter.targetChannel = targetChannel
        simpleCellCounter.localThresholdRadius = localThresholdRadius
        simpleCellCounter.gaussianBlurSigma = gaussianBlurSigma
        simpleCellCounter.shouldRemoveAxons = shouldRemoveAxons
        context.inject(simpleCellCounter)

        val numCellsList = inputImages.map { simpleCellCounter.process(it, cellDiameterRange).count }
        val imageAndCount = inputImages.zip(numCellsList)

        val output = when (outputFormat) {
            SimpleCellCounter.OutputFormat.CSV -> CSVCounterOutput(outputFile)
            SimpleCellCounter.OutputFormat.XML -> XMLCounterOutput(outputFile)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }
        imageAndCount.forEach { output.addCountForFile(it.second, it.first.title) }
        try {
            output.output()
        } catch (te: TransformerException) {
            displayOutputFileErrorDialog(filetype = "XML")
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
        }
    }
}
