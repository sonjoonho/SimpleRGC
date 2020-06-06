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
import simplecolocalization.services.CellDiameterRange
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.DiameterParseException
import simplecolocalization.widgets.AlignedTextWidget

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
        label = "Which plugin would you like to run in batch mode?",
        choices = [PluginChoice.SIMPLE_CELL_COUNTER, PluginChoice.SIMPLE_COLOCALIZATION],
        required = true,
        persist = true,
        style = "radioButtonVertical"
    )
    private var pluginChoice = PluginChoice.SIMPLE_CELL_COUNTER

    @Parameter(
        label = "Input folder",
        required = true,
        persist = true,
        style = "directory"
    )
    private lateinit var inputFolder: File

    @Parameter(
        label = "Batch process files in nested sub-folders?",
        required = true,
        persist = true
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
        label = "Cell morphology channel 1",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    private var targetChannel = 1

    /**
     * Specify the channel for the all cells channel.
     * By default this is the 0 (disabled).
     */
    @Parameter(
        label = "Cell morphology channel 2 (colocalization only, 0 to disable)",
        min = "0",
        stepSize = "1",
        required = true,
        persist = true
    )
    var allCellsChannel = 0

    /**
     * Specify the channel for the transduced cells.
     * By default this is the 2 (green) channel.
     */
    @Parameter(
        label = "Transduction channel (colocalization only)",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    private var transducedChannel = 2

    @Parameter(
        label = "Image processing parameters",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var processingParametersHeader: String

    /**
     * Used during the cell identification stage to filter out cells that are too small
     */
    @Parameter(
        label = "Cell diameter for morphology channel 1 (px)",
        description = "Used as minimum/maximum diameter when identifying cells",
        required = true,
        style = AlignedTextWidget.RIGHT,
        persist = true
    )
    var cellDiameterText = "0.0-30.0"

    /**
     * Used as the size of the window over which the threshold will be locally computed.
     */
    @Parameter(
        label = "Local threshold radius",
        // TODO(#133): Improve this description to make more intuitive.
        description = "The radius of the local domain over which the threshold will be computed.",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = true
    )
    var localThresholdRadius = 20

    /**
     * Used during the cell identification stage to filter out cells that are too small
     */
    @Parameter(
        label = "Cell diameter for morphology channel 2 (px) (colocalization only, if enabled)",
        description = "Used as minimum/maximum diameter when identifying cells",
        required = true,
        style = AlignedTextWidget.RIGHT,
        persist = true
    )
    var allCellDiameterText = "0.0-30.0"

    @Parameter(
        label = "Gaussian blur sigma",
        description = "Sigma value used for blurring the image during the processing," +
            " a lower value is recommended if there is a high cell density",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = true
    )
    private var gaussianBlurSigma = 3.0

    @Parameter(
        label = "Output parameters",
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
        label = "Results output",
        choices = [OutputFormat.CSV, OutputFormat.XML],
        required = true,
        persist = true,
        style = "radioButtonVertical"
    )
    private var outputFormat = OutputFormat.CSV

    @Parameter(
        label = "Output file",
        required = true,
        persist = true,
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

        val cellDiameterRange: CellDiameterRange
        try {
            cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterText)
        } catch (e: DiameterParseException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
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
            PluginChoice.SIMPLE_COLOCALIZATION -> BatchableColocalizer(
                targetChannel,
                transducedChannel,
                allCellsChannel,
                context
            )
            else -> throw IllegalArgumentException("Invalid plugin choice provided")
        }
        // TODO(#136): Think more about allCellsDiameter and where to pass it.
        strategy.process(
            openFiles(files),
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma,
            outputFormat,
            outputFile
        )

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
            file.listFiles()?.filter { f -> !f.isDirectory }?.toList() ?: listOf(file)
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
                MessageDialog(
                    IJ.getInstance(), "Error",
                    """
                    It appears that the Bio-Formats plugin is not installed.
                    Please enable the Fiji update site in order to enable this functionality.
                    """.trimIndent()
                )
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
