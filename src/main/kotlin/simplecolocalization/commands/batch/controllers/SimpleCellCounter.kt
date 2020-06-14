package simplecolocalization.commands.batch.controllers

import ij.IJ
import ij.gui.MessageDialog
import java.io.File
import org.scijava.Context
import simplecolocalization.commands.batch.BatchableColocalizer
import simplecolocalization.services.CellDiameterRange

/** Runs BatchableColocalizer, called in action listener for "Ok" button. */
fun runSimpleColocalizer(
    inputFolder: File?,
    shouldProcessFilesInNestedFolders: Boolean,
    thresholdRadius: Int,
    gaussianBlurSigma: Double,
    outputFormat: String,
    targetChannel: Int,
    transducedChannel: Int,
    allCellsChannel: Int,
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
        val colocalizer = BatchableColocalizer(targetChannel, transducedChannel, allCellsChannel, context)
        colocalizer.process(
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
