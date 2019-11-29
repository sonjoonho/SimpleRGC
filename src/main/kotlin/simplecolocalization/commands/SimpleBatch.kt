package simplecolocalization.commands

import ij.IJ
import ij.gui.MessageDialog
import net.imagej.ImageJ
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import simplecolocalization.services.CellColocalizationService
import simplecolocalization.services.CellSegmentationService
import java.io.File
import java.io.IOException
import java.util.stream.Collectors



@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch Run")
class SimpleBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var uiService: UIService

    @Parameter
    private lateinit var cellSegmentationService: CellSegmentationService

    @Parameter
    private lateinit var cellColocalizationService: CellColocalizationService

    override fun run() {
        // Set batch mode to false.

        // Get the selected folder from somewhere?
        // Maybe use: https://imagej.nih.gov/ij/developer/api/ij/plugin/FolderOpener.html
        // Do the following:
        val dir : File // Get this somehow

        // If fails at some point:
        MessageDialog(IJ.getInstance(), "Error", "There is no file open")

        val files = getAllFiles(dir)
        val lifs = files.filter { f -> f.endsWith(".lif") }
        if (lifs.size > 0) {
            // Create dialog to say: we found n lifs. Please note this plugin
            // is only able to process files in .tif format. If you would like to
            // Process both lifs and tifs, please install the bioformats plugin (link)
            // and use the macro here (link).

            // Would you like to continue?
            // GetBoolean
            return
        }

        val tifs = files.filter { f -> f.endsWith(".tif") }
        // Should we parallelise the below with cheeky coroutines???!!!
        for (tif in tifs) {
            process(tif)
        }
    }

    private fun getAllFiles(file: File): List<File> {
        return file.walkTopDown().filter { f -> !f.isDirectory }.toList()
    }


    private fun process(file: File) {
        // Run selected plugin!
    }

    companion object {
        /**
         * Entry point to directly open the plugin, used for debugging purposes.
         *
         * @throws Exception
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val ij = ImageJ()

            ij.context().inject(CellSegmentationService())
            ij.context().inject(CellColocalizationService())
            ij.launch()
            // We can't do this in this location right?
            // val file: File = ij.ui().chooseFile(null, FileWidget.DIRECTORY_STYLE)
            ij.command().run(SimpleBatch::class.java, true)
        }
    }
}