package simplecolocalization.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import java.io.IOException
import javax.xml.transform.TransformerException
import org.scijava.Context
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.commands.displayOutputFileErrorDialog
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput

class BatchableCellCounter(private val context: Context) : Batchable {
    override fun process(
        inputImages: List<ImagePlus>,
        smallestCellDiameter: Double,
        largestCellDiameter: Double,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    ) {
        val simpleCellCounter = SimpleCellCounter()

        // TODO(sonjoonho): I hate this
        simpleCellCounter.smallestCellDiameter = smallestCellDiameter
        simpleCellCounter.largestCellDiameter = largestCellDiameter
        simpleCellCounter.localThresholdRadius = localThresholdRadius
        simpleCellCounter.gaussianBlurSigma = gaussianBlurSigma
        context.inject(simpleCellCounter)

        val numCellsList = inputImages.map { simpleCellCounter.process(it).count }
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
            "XML".displayOutputFileErrorDialog()
        } catch (ioe: IOException) {
            "".displayOutputFileErrorDialog()
        }
        MessageDialog(
            IJ.getInstance(),
            "Saved",
            "The colocalization results have successfully been saved to the specified file."
        )
    }
}
