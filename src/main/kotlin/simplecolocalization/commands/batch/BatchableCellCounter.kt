package simplecolocalization.commands.batch

import ij.gui.GenericDialog
import java.io.File
import java.io.IOException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.services.counter.output.CSVCounterOutput

class BatchableCellCounter(private val largestCellDiameter: Double, private val outputFormat: String, private val context: Context) : Batchable {
    override fun process(inputFiles: List<File>, outputFile: File) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val preprocessingParameters = PreprocessingParameters(largestCellDiameter = largestCellDiameter)

        val numCellsList = inputFiles.map { simpleCellCounter.countCells(it.absolutePath, preprocessingParameters).size }
        val imageAndCount = inputFiles.zip(numCellsList)

        when (outputFormat) {
            SimpleBatch.OutputFormat.CSV -> {
                val output = CSVCounterOutput(outputFile)
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
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
        } }
}
