package simplecolocalization.commands.batch.controllers

import java.io.File
import java.io.FileNotFoundException
import org.scijava.Context
import simplecolocalization.commands.batch.BatchableCellCounter
import simplecolocalization.services.CellDiameterRange

/** Runs BatchableCellCounter, called in action listener for "Ok" button. */
fun runRGCCounter(
    inputFolder: File?,
    shouldProcessFilesInNestedFolders: Boolean,
    channel: Int,
    thresholdRadius: Int,
    gaussianBlurSigma: Double,
    shouldRemoveAxons: Boolean,
    cellDiameterRange: CellDiameterRange,
    outputFormat: String,
    outputFile: File?,
    context: Context
) {
    if (inputFolder == null) {
        throw FileNotFoundException("No output directory is selected")
    } else if (outputFile == null) {
        throw FileNotFoundException("No output file selected")
    } else if (!inputFolder.exists()) {
        throw FileNotFoundException("The input folder could not be opened. Please create it if it does not already exist")
    }

    val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)
    val cellCounter = BatchableCellCounter(channel, context)

    cellCounter.process(
        openFiles(files),
        cellDiameterRange,
        thresholdRadius,
        gaussianBlurSigma,
        shouldRemoveAxons,
        outputFormat,
        outputFile
    )
}
