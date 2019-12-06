package simplecolocalization.commands

import de.siegmar.fastcsv.writer.CsvWriter
import ij.IJ
import ij.ImagePlus
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.plugin.ZProjector
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
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

    object PluginChoice {
        const val SIMPLE_CELL_COUNTER = "SimpleCellCounter"
        const val SIMPLE_COLOCALIZATION = "SimpleColocalization"
    }

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

    /**
     * Specify the channel for the target cell. ImageJ does not have a way to retrieve
     * the channels available at the parameter initiation stage.
     * By default this is 1 (red) channel.
     */
    @Parameter(
        label = "Target Cell Channel (Colocalization Only)",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    private var targetChannel = 1

    /**
     * Specify the channel for the transduced cells.
     * By default this is the 2 (green) channel.
     */
    @Parameter(
        label = "Transduced Cell Channel (Colocalization Only)",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    private var transducedChannel = 2

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
                if (!outputFile.path.endsWith(".csv", ignoreCase = true)) {
                    outputFile = File("${outputFile.path}.csv")
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
        val simpleColocalization = SimpleColocalization()
        context.inject(simpleColocalization)

        val analyses = tifs.map {
            var image = ImagePlus(it.absolutePath)
            if (image.nSlices > 1) {
                // Flatten slices of the image. This step should probably be done during the preprocessing step - however
                // this operation is not done in-place but creates a new image, which makes this hard.
                image = ZProjector.run(image, "max")
            }

            val imageChannels = ChannelSplitter.split(image)
            if (targetChannel < 1 || targetChannel > imageChannels.size) {
                MessageDialog(
                    IJ.getInstance(),
                    "Error",
                    "Target channel {$targetChannel} does not exist in ${image.originalFileInfo.fileName}. There are ${imageChannels.size} channels available."
                )
                return
            }

            if (transducedChannel < 1 || transducedChannel > imageChannels.size) {
                MessageDialog(
                    IJ.getInstance(),
                    "Error",
                    "Transduced channel {$transducedChannel does not exist in ${image.originalFileInfo.fileName}. There are ${imageChannels.size} channels available."
                )
                return
            }

            simpleColocalization.analyseColocalization(imageChannels[targetChannel], imageChannels[transducedChannel])
        }

        val fileNameAndAnalysis = tifs.map { it.name }.zip(analyses)
        val csvWriter = CsvWriter()
        val outputData = mutableListOf(arrayOf("File Name", "Total Target Cells", "Total Transduced Target Cells"))
        outputData.addAll(fileNameAndAnalysis.map {
            // TODO(Kelvin): The total target cells here should be the result of cell counting, not transduction.
            // This needs a major change in ColocalizationResult.
            val totalTargetCells = (it.second.partitionedCells.overlapping.size + it.second.partitionedCells.disjoint.size).toString()
            val totalTransducedTargetCells = it.second.partitionedCells.overlapping.size.toString()
            arrayOf(it.first, totalTargetCells, totalTransducedTargetCells)
        })
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
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
