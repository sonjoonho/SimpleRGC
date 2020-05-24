package simplecolocalization.commands.batch

import ij.ImagePlus
import ij.gui.GenericDialog
import java.io.File
import java.io.IOException
import javax.xml.transform.TransformerException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.services.CellDiameterRange
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput

class BatchableCellCounter(private val targetChannel: Int, private val context: Context) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        cellDiameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    ) {
        val simpleCellCounter = SimpleCellCounter()

        simpleCellCounter.targetChannel = targetChannel
        simpleCellCounter.localThresholdRadius = localThresholdRadius
        simpleCellCounter.gaussianBlurSigma = gaussianBlurSigma
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
            displayErrorDialog(fileType = "XML")
        } catch (ioe: IOException) {
            displayErrorDialog()
        }
    }

    // TODO(tiger-cross): Reduce duplication here.
    private fun displayErrorDialog(fileType: String = "") {
        GenericDialog("Error").apply {
            addMessage("Unable to save results to $fileType file. Ensure the output file is not currently open by other programs and try again.")
            hideCancelButton()
            showDialog()
        }
    }
}
