package simplecolocalization.commands.batch.controllers

import ij.IJ
import ij.gui.MessageDialog
import java.io.File
import org.scijava.Context
import simplecolocalization.commands.batch.BatchableCellCounter
import simplecolocalization.services.CellDiameterRange

/** Runs BatchableCellCounter, called in action listener for "Ok" button. */
fun runSimpleCellCounter(
    inputFolder: File?,
    shouldProcessFilesInNestedFolders: Boolean,
    channel: Int,
    thresholdRadius: Int,
    gaussianBlurSigma: Double,
    outputFormat: String,
    outputFile: File?,
    context: Context
) {
    if (inputFolder == null) {
        MessageDialog(
            IJ.getInstance(), "Error", "No input folder selected"
        )
    } else if (outputFile == null) {
        MessageDialog(
            IJ.getInstance(), "Error", "No output file selected"
        )
    } else if (!inputFolder.exists()) {
        MessageDialog(
            IJ.getInstance(), "Error",
            "The input folder could not be opened. Please create it if it does not already exist"
        )
    } else {
        val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)

        val cellCounter = BatchableCellCounter(channel, context)

        // TODO: Use the user input cell diameter range
        cellCounter.process(
            openFiles(files),
            CellDiameterRange(0.0, 100.0),
            thresholdRadius,
            gaussianBlurSigma,
            outputFormat,
            outputFile
        )
        MessageDialog(
            IJ.getInstance(),
            "Saved",
            "The batch processing results have successfully been saved to the specified file."
        )
    }
}
