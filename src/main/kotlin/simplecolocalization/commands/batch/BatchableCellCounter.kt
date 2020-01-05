package simplecolocalization.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import java.io.File
import java.io.IOException
import javax.xml.transform.TransformerException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput

class BatchableCellCounter(private val context: Context) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        outputFormat: String,
        outputFile: File,
        preprocessingParameters: PreprocessingParameters
    ) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val numCellsList = inputImages.map { simpleCellCounter.process(it, preprocessingParameters).count }
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
