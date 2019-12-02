package simplecolocalization.commands

import ij.IJ
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
import org.scijava.widget.NumberWidget
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.ImageJTableCounterOutput

object PluginChoice {
    const val SIMPLE_CELL_COUNTER = "SimpleCellCounter"
    const val SIMPLE_COLOCALIZATION = "SimpleColocalization"
}

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch Process")
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
        label = "Output File (if saving in CSV):",
        style = "save",
        required = false
    )
    private var outputFile: File? = null

    /**
     * Used during the cell segmentation stage to perform local thresholding or
     * background subtraction.
     */
    @Parameter(
        label = "Largest Cell Diameter",
        description = "Value we use to apply the rolling ball algorithm to subtract " +
            "the background when thresholding",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var largestCellDiameter = 30.0

    @Parameter(
        label = "Batch Process files in nested folders ?",
        required = true
    )
    private var processFilesInNestedFolders: Boolean = true

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
            MessageDialog(IJ.getInstance(), "Error", "Input Folder/File specified does not exist.")
            return
        }

        val files = getAllFiles(file, processFilesInNestedFolders)

        val lifs = files.filter { it.extension == "lif" }
        if (lifs.isNotEmpty()) {

            val dialog = GenericDialog(".LIF files found")

            dialog.addMessage("We found ${lifs.size} files with the .LIF extension. \n" +
                "Please note that this plugin will skip over files in the .LIF format. \n" +
                "Please refer to this plugin's documentation on how to automatically " +
                "batch convert .LIF files to the accepted .TIF extension.")

            dialog.addMessage("Continue to process only .TIF images in your input directory")

            dialog.showDialog()

            if (dialog.wasCanceled()) {
                return
            }
        }

        val tifs = files.filter { it.extension == "tif" || it.extension == "tiff" }

        process(tifs)
    }

    private fun getAllFiles(file: File, processFilesInNestedFolders: Boolean): List<File> {
        return if (processFilesInNestedFolders) {
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

        val preprocessingParameters = PreprocessingParameters(largestCellDiameter = largestCellDiameter)

        val numCellsList = tifs.map { simpleCellCounter.countCells(it.absolutePath, preprocessingParameters).size }
        val imageAndCount = tifs.zip(numCellsList)

        when (outputDestination) {
            OutputDestination.DISPLAY -> {
                val output = ImageJTableCounterOutput(uiService)
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
                output.show()
            }
            OutputDestination.CSV -> {
                val output = CSVCounterOutput(outputFile!!)
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
                output.save()
            }
        }
    }

    // TODO: Implement batch processing for SimpleColocalization
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