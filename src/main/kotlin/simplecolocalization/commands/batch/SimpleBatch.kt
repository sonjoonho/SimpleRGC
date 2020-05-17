package simplecolocalization.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.io.Opener
import java.io.File
import loci.formats.UnknownFormatException
import loci.plugins.BF
import loci.plugins.`in`.ImporterOptions
import net.imagej.ImageJ
import org.scijava.Context
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.NumberWidget
import simplecolocalization.services.CellSegmentationService

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch")
class SimpleBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var uiService: UIService

    @Parameter
    private lateinit var context: Context

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
        label = "Batch process files in nested sub-folders?",
        required = true
    )
    private var shouldProcessFilesInNestedFolders: Boolean = true

    @Parameter(
        label = "<html><div align=\"right\">\nWhen performing batch colocalization, ensure that <br />all input images have the same channel ordering as<br />specified below.</div></html>",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private var colocalizationInstruction = ""

    /**
     * Specify the channel for the target cell. ImageJ does not have a way to retrieve
     * the channels available at the parameter initialisation stage.
     * By default this is 1 (red) channel.
     */
    @Parameter(
        label = "Cell Morphology Channel 1",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    private var targetChannel = 1

    /**
     * Specify the channel for the all cells channel.
     * By default this is the 0 (disabled).
     */
    @Parameter(
        label = "Cell Morphology Channel 2 (Colocalization Only, 0 to disable):",
        min = "0",
        stepSize = "1",
        required = true,
        persist = false
    )
    var allCellsChannel = 0

    /**
     * Specify the channel for the transduced cells.
     * By default this is the 2 (green) channel.
     */
    @Parameter(
        label = "Transduction Channel (Colocalization Only)",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    private var transducedChannel = 2

    @Parameter(
        label = "Preprocessing Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var preprocessingParamsHeader: String

    /**
     * Used during the cell segmentation stage to perform local thresholding or
     * background subtraction.
     */
    @Parameter(
        label = "Largest Cell Diameter for Morphology Channel 1 (px)",
        description = "Value we use to apply the rolling ball algorithm to subtract the background when thresholding",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var largestCellDiameter = 30.0

    @Parameter(
        label = "Largest Cell Diameter for Morphology Channel 2 (px) (colocalization, only if channel enabled)",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var largestAllCellsDiameter = 30.0

    @Parameter(
        label = "Output Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var outputParametersHeader: String

    /**
     * The user can optionally output the results to a file.
     *
     * TODO(Arjun): Reinstate a parameter for this once display output is built
     */
    object OutputFormat {
        const val CSV = "Save as CSV file"
        const val XML = "Save as XML file"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputFormat.CSV, OutputFormat.XML],
        required = true,
        persist = false,
        style = "radioButtonVertical"
    )
    private var outputFormat = OutputFormat.CSV

    @Parameter(
        label = "Output file:",
        required = true,
        persist = false,
        style = "save"
    )
    private lateinit var outputFile: File

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
            PluginChoice.SIMPLE_CELL_COUNTER -> BatchableCellCounter(targetChannel, context)
            PluginChoice.SIMPLE_COLOCALIZATION -> BatchableColocalizer(targetChannel, transducedChannel, allCellsChannel, context)
            else -> throw IllegalArgumentException("Invalid plugin choice provided")
        }
        // TODO(tiger-cross): Think more about allCellsDiameter and where to pass it.
        strategy.process(openFiles(files), largestCellDiameter, outputFormat, outputFile)

        MessageDialog(
            IJ.getInstance(),
            "Saved",
            "The batch processing results have successfully been saved to the specified file."
        )
    }

        private fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
            return if (shouldProcessFilesInNestedFolders) {
                file.walkTopDown().filter { f -> !f.isDirectory }.toList()
            } else {
                file.listFiles()?.toList() ?: listOf(file)
            }
        }

    private fun openFiles(inputFiles: List<File>): List<ImagePlus> {
        /*
        First, we attempt to use the default ImageJ Opener. The ImageJ Opener falls back to a plugin called
        HandleExtraFileTypes when it cannot open a file - which attempts to use Bio-Formats when it encounters a LIF.
        Unfortunately, the LociImporter (what Bio-Formats uses) opens a dialog box when it does this. It does
        support a "windowless" option, but it's not possible to pass this option (or any of our desired options) through
        HandleExtraFileTypes. So instead, we limit the scope of possible file types by supporting native ImageJ formats
        (Opener.types), preventing HandleExtraFileTypes from being triggered, and failing this fall back to calling the
        Bio-Formats Importer manually. This handles the most common file types we expect to encounter.

        Also, note that Opener returns null when it fails to open a file, whereas the Bio-Formats Importer throws an
        UnknownFormatException`. To simplify the logic, an UnknownFormatException is thrown when Opener returns null.
        */
        val opener = Opener()
        val inputImages = mutableListOf<ImagePlus>()

        for (file in inputFiles) {

            try {
                if (Opener.types.contains(file.extension)) {
                    val image = opener.openImage(file.absolutePath) ?: throw UnknownFormatException()
                    inputImages.add(image)
                } else {
                    val options = ImporterOptions()
                    options.id = file.path
                    options.colorMode = ImporterOptions.COLOR_MODE_COMPOSITE
                    options.isAutoscale = true
                    options.setOpenAllSeries(true)

                    // Note that the call to BF.openImagePlus returns an array of images because a single LIF file can
                    // contain multiple series.
                    inputImages.addAll(BF.openImagePlus(options))
                }
            } catch (e: UnknownFormatException) {
                logService.warn("Skipping file with unsupported type \"${file.name}\"")
            } catch (e: NoClassDefFoundError) {
                MessageDialog(IJ.getInstance(), "Error",
                    """
                    It appears that the Bio-Formats plugin is not installed.
                    Please enable the Fiji update site in order to enable this functionality.
                    """.trimIndent())
            }
        }

        return inputImages
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
