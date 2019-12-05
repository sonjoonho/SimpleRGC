package simplecolocalization.commands

import ij.IJ
import ij.gui.GenericDialog
import ij.gui.MessageDialog
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
import java.io.IOException

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
     *
     * TODO(Arjun): Reinstate a parameter for this once display output is built
     */
    object OutputDestination {
        const val CSV = "Save as CSV file"
    }
    private var outputDestination = OutputDestination.CSV

    @Parameter(
        label = "Which plugin do you want to run in batch mode?",
        choices = [PluginChoice.SIMPLE_CELL_COUNTER, PluginChoice.SIMPLE_COLOCALIZATION],
        required = true,
        style = "radioButtonVertical"
    )
    private var pluginChoice = PluginChoice.SIMPLE_CELL_COUNTER

    @Parameter(
        label = "Input folder:",
        required = true,
        persist = false,
        style = "directory"
    )
    private lateinit var inputFolder: File

    @Parameter(
        label = "Output file (CSV):",
        required = true,
        persist = false,
        style = "save"
    )
    private lateinit var outputFile: File

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
        label = "Batch process files in nested sub-folders?",
        required = true
    )
    private var shouldProcessFilesInNestedFolders: Boolean = true

    override fun run() {
        if (!inputFolder.exists()) {
            MessageDialog(IJ.getInstance(), "Error",
                "The input folder could not be opened. Please create it if it does not already exist")
            return
        }

        // Validate output file extension
        when (outputDestination) {
            OutputDestination.CSV -> {
                if (!outputDestination.endsWith(".csv", ignoreCase = true)) {
                    outputDestination = "$outputDestination.csv"
                }
            }
        }

        val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)

        val tifs = files.filter { it.extension == "tif" || it.extension == "tiff" }
        val lifs = files.filter { it.extension == "lif" }

        if (lifs.isNotEmpty()) {
            GenericDialog(".LIF Files Found").apply {
                addMessage("""
                    We found ${lifs.size} file(s) with the .LIF extension. 
                    Please note that this plugin will skip over files in the .LIF format. 
                    Please refer to this plugin's documentation on how to automatically batch convert .LIF files to the accepted .TIF extension.
                    """.trimIndent()
                )
                addMessage("Continue to process only .TIF images in your input directory.")
                showDialog()
                if (wasCanceled()) {
                    return
                }
            }
        }

        process(tifs, outputFile)
    }

    private fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
        return if (shouldProcessFilesInNestedFolders) {
            file.walkTopDown().filter { f -> !f.isDirectory }.toList()
        } else {
            file.listFiles()?.toList() ?: listOf(file)
        }
    }

    private fun process(tifs: List<File>, outputFile: File) {
        when (pluginChoice) {
            PluginChoice.SIMPLE_CELL_COUNTER -> processSimpleCellCounter(tifs, outputFile)
            PluginChoice.SIMPLE_COLOCALIZATION -> processSimpleColocalization(tifs, outputFile)
        }
    }

    private fun processSimpleCellCounter(tifs: List<File>, outputFile: File) {
        val simpleCellCounter = SimpleCellCounter()
        context.inject(simpleCellCounter)

        val preprocessingParameters = PreprocessingParameters(largestCellDiameter = largestCellDiameter)

        val numCellsList = tifs.map { simpleCellCounter.countCells(it.absolutePath, preprocessingParameters).size }
        val imageAndCount = tifs.zip(numCellsList)

        when (outputDestination) {
            OutputDestination.CSV -> {
                val output = CSVCounterOutput(outputFile)
                imageAndCount.forEach { output.addCountForFile(it.second, it.first.name) }
                try {
                    output.save()
                } catch (e: IOException) {
                    GenericDialog("Error").apply {
                        addMessage("Unable to save results to CSV file. Ensure the output file is not currently open by other programs and try again.")
                        hideCancelButton()
                        showDialog()
                    }
                }
            }
        }
    }

    private fun processSimpleColocalization(tifs: List<File>, outputFile: File) {
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
