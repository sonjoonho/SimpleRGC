package simplecolocalization.commands.batch

import ij.ImagePlus
import ij.gui.GenericDialog
import java.io.File
import java.io.IOException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.services.counter.output.CSVCounterOutput

class BatchableCellCounter(private val context: Context) : Batchable {
    override fun process(inputImages: List<ImagePlus>, outputFile: File, preprocessingParameters: PreprocessingParameters) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val numCellsList = inputImages.map { simpleCellCounter.process(it, preprocessingParameters).count }
        val imageAndCount = inputImages.zip(numCellsList)

        val output = CSVCounterOutput(outputFile)
        imageAndCount.forEach { output.addCountForFile(it.second, it.first.title) }
        try {
            output.save()
        } catch (e: IOException) {
            GenericDialog("Error").apply {
                addMessage("Unable to save results to CSV file. Ensure the output file is not currently open by other programs and try again.")
                hideCancelButton()
                showDialog()
            }
        }
        }
}
