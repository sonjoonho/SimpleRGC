package simplergc.commands.batch.controllers

import java.awt.event.ActionListener
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.concurrent.thread
import org.scijava.app.StatusService
import simplergc.commands.batch.Batchable
import simplergc.commands.batch.models.RGCParameters
import simplergc.commands.batch.views.RGCView
import simplergc.comparators.AlphanumFileComparator
import simplergc.services.DiameterParseException

abstract class RGCController(private val statusService: StatusService) {

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
        val orderedFiles = files.sortedWith(AlphanumFileComparator)

        processor.process(
            openFiles(orderedFiles),
            p.cellDiameterRange,
            p.thresholdRadius,
            p.gaussianBlurSigma,
            p.outputFormat,
            p.outputFile!!
        )

        statusService.showStatus(100, 100, "Finished batch processing!")
    }

    fun okButton(): ActionListener {
        return ActionListener {
            val p: RGCParameters
            try {
                p = harvestParameters()
            } catch (dpe: DiameterParseException) {
                view.dialog("Error", dpe.message ?: "Could not parse cell diameter")
                return@ActionListener
            }

            saveParameters(p)

            try {
                thread(start = true) {
                    process(p)
                    view.dialog("Saved", "The batch processing results have successfully been saved to ${p.outputFile}")
                }
            } catch (e: FileNotFoundException) {
                view.dialog("Error", e.message ?: "File not found.")
            } catch (ioe: IOException) {
                view.dialog("Error", ioe.message ?: "File could not be opened/saved.")
            }
            view.close()
        }
    }
}
