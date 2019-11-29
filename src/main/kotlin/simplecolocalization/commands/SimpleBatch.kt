package simplecolocalization.commands

import ij.IJ
import ij.gui.GenericDialog
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

    @Parameter(
        label = "Batch Process files in subdirectories recursively ?",
        required = true
    )
    private var recursive: Boolean = false

    override fun run() {
        // Set batch mode to false.

        // Get the selected folder from somewhere?
        // Maybe use: https://imagej.nih.gov/ij/developer/api/ij/plugin/FolderOpener.html
        // Do the following:
        val path = IJ.getDirectory("current")
        val file = File(path)

        if (!file.exists()) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }

        val files = getAllFiles(file, recursive)

        val lifs = files.filter { f -> f.endsWith(".lif") }
        if (lifs.isNotEmpty()) {

            val dialog = GenericDialog("Found lifs")

            dialog.addMessage("We found ${lifs.size} files with the .lif extension. \n" +
                "Please note this plugin is only able to process files that are in .tif format. \n" +
                "If you would like to process both lifs, please install the Bioformats plugin \n")

            dialog.addMessage("Please press OK if you would like to continue and process tifs")

            dialog.showDialog()

            if (dialog.wasCanceled()) {
                return
            }
        }

        val tifs = files.filter { f -> f.endsWith(".tif") or f.endsWith(".tiff") }
        // Should we parallelise the below with cheeky coroutines???!!!

        for (tif in tifs) {
            process(tif)
        }
    }

    private fun getAllFiles(file: File, recursive: Boolean): List<File> {
        return if (recursive) {
            file.walkTopDown().filter { f -> !f.isDirectory }.toList()
        } else {
            file.listFiles()?.toList() ?: listOf(file)
        }
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

            ij.command().run(SimpleBatch::class.java, true)
        }
    }
}