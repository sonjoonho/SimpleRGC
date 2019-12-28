package simplecolocalization.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.io.Opener
import ij.plugin.ZProjector
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
    object OutputFormat {
        const val CSV = "Save as CSV file"
    }

    private var outputFormat = OutputFormat.CSV

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
            MessageDialog(
                IJ.getInstance(), "Error",
                "The input folder could not be opened. Please create it if it does not already exist"
            )
            return
        }

        // Validate output file extension.
        when (outputFormat) {
            OutputFormat.CSV -> {
                if (!outputFile.path.endsWith(".csv", ignoreCase = true)) {
                    outputFile = File("${outputFile.path}.csv")
                }
            }
        }

        val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)

        val strategy = when (pluginChoice) {
            PluginChoice.SIMPLE_CELL_COUNTER -> BatchableCellCounter(outputFormat, context)
            PluginChoice.SIMPLE_COLOCALIZATION -> BatchableColocalizer(targetChannel, transducedChannel, context)
            else -> throw IllegalArgumentException("Invalid plugin choice provided")
        }

        strategy.process(openFiles(files), outputFile, PreprocessingParameters(largestCellDiameter))
    }

        private fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
            return if (shouldProcessFilesInNestedFolders) {
                file.walkTopDown().filter { f -> !f.isDirectory }.toList()
            } else {
                file.listFiles()?.toList() ?: listOf(file)
            }
        }

    private fun openFiles(inputFiles: List<File>): List<ImagePlus> {
        val opener = Opener()

        return inputFiles.mapNotNull {
            val image: ImagePlus? = opener.openImage(it.absolutePath) ?: return@mapNotNull null

            val flatImage = if (image!!.nSlices > 1) {
                // Flatten slices of the image. This step should probably be done during the preprocessing step - however
                // this operation is not done in-place but creates a new image, which makes this hard.
                ZProjector.run(image, "max")
            } else {
                image
            }

            return@mapNotNull flatImage
        }
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
