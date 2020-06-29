package simplergc.commands.batch.controllers

import java.io.File
import java.io.FileNotFoundException
import org.scijava.Context
import simplergc.commands.batch.BatchableColocalizer
import simplergc.services.CellDiameterRange

/** Runs BatchableColocalizer, called in action listener for "Ok" button. */
fun runRGCTransduction(
    inputFolder: File?,
    shouldProcessFilesInNestedFolders: Boolean,
    thresholdRadius: Int,
    gaussianBlurSigma: Double,
    targetChannel: Int,
    shouldRemoveAxonsFromTargetChannel: Boolean,
    transducedChannel: Int,
    shouldRemoveAxonsFromTransductionChannel: Boolean,
    cellDiameterRange: CellDiameterRange,
    outputFormat: String,
    outputFile: File?,
    context: Context
) {
    if (inputFolder == null) {
        throw FileNotFoundException("No input directory is selected")
    } else if (outputFile == null) {
        throw FileNotFoundException("No output file selected")
    } else if (!inputFolder.exists()) {
        throw FileNotFoundException("The input folder could not be opened. Please create it if it does not already exist")
    }

    val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)
    val colocalizer = BatchableColocalizer(
        targetChannel,
        shouldRemoveAxonsFromTargetChannel,
        transducedChannel,
        shouldRemoveAxonsFromTransductionChannel,
        context
    )
    colocalizer.process(
        openFiles(files),
        cellDiameterRange,
        thresholdRadius,
        gaussianBlurSigma,
        outputFormat,
        outputFile
    )
}
