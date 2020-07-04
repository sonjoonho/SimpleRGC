package simplergc.commands.batch.controllers

import java.awt.event.ActionListener
import java.io.FileNotFoundException
import simplergc.commands.batch.Batchable
import simplergc.commands.batch.models.RGCParameters
import simplergc.commands.batch.views.RGCView

abstract class RGCController {
    abstract val view: RGCView
    abstract fun makeProcessor(p: RGCParameters): Batchable
    abstract fun harvestParameters(): RGCParameters
    abstract fun saveParameters(p: RGCParameters)
    fun process(p: RGCParameters) {
        if (p.inputDirectory == null) {
            throw FileNotFoundException("No input directory is selected")
        } else if (p.outputFile == null) {
            throw FileNotFoundException("No output file selected")
        } else if (!p.inputDirectory!!.exists()) {
            throw FileNotFoundException("The input folder could not be opened. Please create it if it does not already exist")
        }
        val processor = makeProcessor(p)

        val files = getAllFiles(p.inputDirectory!!, p.shouldProcessFilesInNestedFolders)

        processor.process(
            openFiles(files),
            p.cellDiameterRange,
            p.thresholdRadius,
            p.gaussianBlurSigma,
            p.outputFormat,
            p.outputFile!!
        )
    } fun okButton(): ActionListener {
        return ActionListener {
            val p = harvestParameters()
            saveParameters(p)

            try {
                process(p)
                view.dialog("Saved", "The batch processing results have successfully been saved to the specified file")
            } catch (e: FileNotFoundException) {
                view.dialog("Error", e.message ?: "An error occurred")
            }
        }
    } }
