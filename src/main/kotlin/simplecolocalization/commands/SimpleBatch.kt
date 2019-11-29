package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.io.DirectoryChooser
import java.io.File
import net.imagej.ImageJ
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.counter.output.ImageJTableCounterOutput

object PluginChoice {
    const val SIMPLE_CELL_COUNTER = "SimpleCellCounter"
    const val SIMPLE_COLOCALIZATION = "SimpleColocalization"
}

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch Run")
class SimpleBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var uiService: UIService

    @Parameter
    private lateinit var context: Context

    /**
     * The user can optionally output the results to a file.
     */
    object OutputDestination {
        const val DISPLAY = "Display in table"
        const val CSV = "Save as CSV file"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputDestination.DISPLAY, OutputDestination.CSV],
        required = true,
        persist = false,
        style = "radioButtonVertical"
    )
    private var outputDestination = OutputDestination.DISPLAY

    @Parameter(
        label = "Batch Process files in subdirectories recursively ?",
        required = true
    )
    private var recursive: Boolean = false

    @Parameter(
        label = "Which plugin do you want to run in Batch Mode ?",
        choices = [PluginChoice.SIMPLE_CELL_COUNTER, PluginChoice.SIMPLE_COLOCALIZATION],
        required = true
    )
    private var pluginChoice = PluginChoice.SIMPLE_CELL_COUNTER

    override fun run() {

        val directoryChooser = DirectoryChooser("Select Input Folder")

        val path = directoryChooser.directory
        val file = File(path)

        if (!file.exists()) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }

        val files = getAllFiles(file, recursive)

        val lifs = files.filter { it.extension == "lif" }
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

        val tifs = files.filter { it.extension == "tif" || it.extension == "tiff" }

        process(tifs)
    }

    private fun getAllFiles(file: File, recursive: Boolean): List<File> {
        return if (recursive) {
            file.walkTopDown().filter { f -> !f.isDirectory }.toList()
        } else {
            file.listFiles()?.toList() ?: listOf(file)
        }
    }

    private fun process(tifs: List<File>) {
        when (pluginChoice) {
            PluginChoice.SIMPLE_CELL_COUNTER -> processSimpleCellCounter(tifs)
            PluginChoice.SIMPLE_COLOCALIZATION -> processSimpleColocalization(tifs)
        }
    }

    private fun processSimpleCellCounter(tifs: List<File>) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val preprocessingParameters = PreprocessingParameters()

        val numCellsList = tifs.map { simpleCellCounter.countCells(ImagePlus(it.absolutePath), preprocessingParameters) }.map { it.size }
        val imageAndCount = tifs.zip(numCellsList)

        when (outputDestination) {
            OutputDestination.DISPLAY -> {
                val output = ImageJTableCounterOutput(uiService)
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
                output.show()
            }
        }
    }

    private fun processSimpleColocalization(tifs: List<File>) {
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
            ij.launch()

            ij.command().run(SimpleBatch::class.java, true)
        }
    }
}
