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

object PluginChoice {
    const val SIMPLE_CELL_COUNTER = "SimpleCellCounter"
    const val SIMPLE_COLOCALIZATION = "SimpleColocalization"
}

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch")
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
        const val CSV = "Save as CSV file"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputDestination.CSV],
        required = true,
        persist = false,
        style = "radioButtonVertical"
    )
    private var outputDestination = OutputDestination.CSV

    @Parameter(
        label = "Output File Name:",
        description = "Please specify the name of the output file. Leaving this empty will save a CSV with the same name as the directory you choose as input.",
        required = false,
        persist = false
    )
    private var outputFileName = ""

    /**
     * Used during the cell segmentation stage to perform local thresholding or
     * background subtraction.
     */
    @Parameter(
        label = "Largest Cell Diameter",
        description = "Value we use to apply the rolling ball algorithm to subtract the background when thresholding",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var largestCellDiameter = 30.0

    @Parameter(
        label = "Batch Process files in nested folders?",
        required = true
    )
    private var shouldProcessFilesInNestedFolders: Boolean = true

    @Parameter(
        label = "Which plugin do you want to run in Batch Mode?",
        choices = [PluginChoice.SIMPLE_CELL_COUNTER, PluginChoice.SIMPLE_COLOCALIZATION],
        required = true
    )
    private var pluginChoice = PluginChoice.SIMPLE_CELL_COUNTER

    override fun run() {

        val directoryChooser = DirectoryChooser("Select Input Folder")

        val path = directoryChooser.directory
        val file = File(path)

        val outputPath = if (outputFileName.isBlank()) {
            "$path${file.name}.csv"
        } else {
            "$path$outputFileName.csv"
        }

        if (!file.exists()) {
            MessageDialog(IJ.getInstance(), "Error",
                "The input folder could not be opened. Please create it if it does not already exist")
            return
        }

        val files = getAllFiles(file, shouldProcessFilesInNestedFolders)

        val tifs = files.filter { it.extension == "tif" || it.extension == "tiff" }
        val lifs = files.filter { it.extension == "lif" }

        if (lifs.isNotEmpty()) {

            val dialog = GenericDialog(".LIF Files Found")

            dialog.addMessage(
                """
                We found ${lifs.size} file(s) with the .LIF extension. 
                Please note that this plugin will skip over files in the .LIF format. 
                Please refer to this plugin's documentation on how to automatically batch convert .LIF files to the accepted .TIF extension.
                """.trimIndent()
            )

            dialog.addMessage("Continue to process only .TIF images in your input directory.")

            dialog.showDialog()

            if (dialog.wasCanceled()) {
                return
            }
        }

        process(tifs, outputPath)
    }

    private fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
        return if (shouldProcessFilesInNestedFolders) {
            file.walkTopDown().filter { f -> !f.isDirectory }.toList()
        } else {
            file.listFiles()?.toList() ?: listOf(file)
        }
    }

    private fun process(tifs: List<File>, outputName: String) {
        when (pluginChoice) {
            PluginChoice.SIMPLE_CELL_COUNTER -> processSimpleCellCounter(tifs, outputName)
            PluginChoice.SIMPLE_COLOCALIZATION -> processSimpleColocalization(tifs, outputName)
        }
    }

    private fun processSimpleCellCounter(tifs: List<File>, outputName: String) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val preprocessingParameters = PreprocessingParameters(largestCellDiameter = largestCellDiameter)

        val numCellsList = tifs.map { simpleCellCounter.countCells(it.absolutePath, preprocessingParameters).size }
        val imageAndCount = tifs.zip(numCellsList)

        when (outputDestination) {
            OutputDestination.CSV -> {
                val output = CSVCounterOutput(File(outputName))
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
                output.save()
            }
        }
    }

    private fun processSimpleColocalization(tifs: List<File>, outputName: String) {
        // TODO("Batch SimpleColocalization unimplemented")
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
