package simplergc.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.plugin.frame.RoiManager
import java.io.File
import java.io.IOException
import net.imagej.ImageJ
import org.apache.commons.io.FilenameUtils
import org.scijava.ItemVisibility
import org.scijava.app.StatusService
import org.scijava.command.Command
import org.scijava.command.Previewable
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.FileWidget
import org.scijava.widget.NumberWidget
import simplergc.services.CellDiameterRange
import simplergc.services.CellSegmentationService
import simplergc.services.DiameterParseException
import simplergc.services.Parameters
import simplergc.services.colocalizer.PositionedCell
import simplergc.services.colocalizer.addToRoiManager
import simplergc.services.colocalizer.drawCells
import simplergc.services.colocalizer.resetRoiManager
import simplergc.services.counter.output.CsvCounterOutput
import simplergc.services.counter.output.ImageJTableCounterOutput
import simplergc.services.counter.output.XlsxCounterOutput
import simplergc.widgets.AlignedTextWidget

/**
 * Segments and counts cells which are almost circular in shape which are likely
 * to overlap.
 *
 * When this plugin is started in a graphical context (as opposed to the command
 * line), a dialog box is opened with the script parameters below as input.
 *
 * [run] contains the main pipeline, which runs only after the script parameters
 * are populated.
 */
@Plugin(type = Command::class, menuPath = "Plugins > Simple RGC > RGC Counter")
class RGCCounter : Command, Previewable {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var statusService: StatusService

    @Parameter
    private lateinit var cellSegmentationService: CellSegmentationService

    /**
     * Entry point for UI operations, automatically handling both graphical and
     * headless use of this plugin.
     */
    @Parameter
    private lateinit var uiService: UIService

    @Parameter(
        label = "Morphology channel",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    var targetChannel = 1

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
        label = "Cell diameter (px)",
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

    @Parameter(
        label = "Gaussian blur sigma",
        description = "Sigma value used for blurring the image during the processing," +
            " a lower value is recommended if there are lots of cells densely packed together",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = true
    )
    var gaussianBlurSigma = 3.0

    @Parameter(
        label = "Exclude axons",
        required = true,
        persist = true
    )
    var shouldRemoveAxons: Boolean = false

    @Parameter(
        label = "Output parameters",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var outputParametersHeader: String

    /**
     * The user can optionally output the results to a file.
     */
    object OutputFormat {
        const val DISPLAY = "Display in ImageJ"
        const val XLSX = "Save as XLSX file (Recommended)"
        const val CSV = "Save as a CSV file"
    }

    @Parameter(
        label = "Results output",
        choices = [OutputFormat.DISPLAY, OutputFormat.XLSX, OutputFormat.CSV],
        required = true,
        persist = true,
        style = "radioButtonVertical"
    )
    private var outputFormat = OutputFormat.DISPLAY

    @Parameter(
        label = "Output File (if saving)",
        style = "save",
        required = false
    )
    private var outputFile: File? = null

    @Parameter(
        visibility = ItemVisibility.INVISIBLE,
        persist = false,
        callback = "previewChanged"
    )
    private var preview: Boolean = false

    data class CounterResult(val count: Int, val cells: List<PositionedCell>)

    /** Runs after the parameters above are populated. */
    override fun run() {
        val image = WindowManager.getCurrentImage()
        if (image == null) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }
        val diameterRange: CellDiameterRange
        try {
            diameterRange = CellDiameterRange.parseFromText(cellDiameterText)
        } catch (e: DiameterParseException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
            return
        }

        statusService.showStatus(0, 100, "Starting...")

        if (outputFormat != OutputFormat.DISPLAY && outputFile == null) {
            val path = image.originalFileInfo.directory
            val extension = when (outputFormat) {
                OutputFormat.CSV -> "csv"
                OutputFormat.XLSX -> "xlsx"
                else -> throw IllegalArgumentException("Invalid output type provided")
            }
            val name = FilenameUtils.removeExtension(image.originalFileInfo.fileName) + ".$extension"
            outputFile = File(path + name)
            if (!outputFile!!.createNewFile()) {
                val dialog = GenericDialog("Warning")
                dialog.addMessage("Overwriting file \"$name\"")
                dialog.showDialog()
                if (dialog.wasCanceled()) return
            }
        }

        resetRoiManager()

        val result = try {
            process(image, diameterRange)
        } catch (e: ChannelDoesNotExistException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
            return
        }

        statusService.showStatus(100, 100, "Done!")

        writeOutput(result.count, image.title, diameterRange)

        image.show()
        addToRoiManager(result.cells)
    }

    private fun writeOutput(numCells: Int, file: String, cellDiameterRange: CellDiameterRange) {
        val counterParameters = Parameters.Counter(
            targetChannel,
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma
        )
        val output = when (outputFormat) {
            OutputFormat.DISPLAY -> ImageJTableCounterOutput(uiService)
            OutputFormat.XLSX -> XlsxCounterOutput(outputFile!!, counterParameters)
            OutputFormat.CSV -> CsvCounterOutput(outputFile!!, counterParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        output.addCountForFile(numCells, file)

        try {
            output.output()
        } catch (ioe: IOException) {
            displayErrorDialog(ioe.message)
        }

        // The cell counting results are clearly displayed if the output
        // destination is set to DISPLAY, however, a visual confirmation
        // is useful if the output is saved to file.
        if (outputFormat != OutputFormat.DISPLAY) {
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The cell counting results have successfully been saved to the specified file."
            )
        }
    }

    /** Processes single image. */
    fun process(image: ImagePlus, diameterRange: CellDiameterRange): CounterResult {
        val imageChannels = ChannelSplitter.split(image)
        if (targetChannel < 1 || targetChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Target channel selected ($targetChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        val cells = cellSegmentationService.extractCells(
            imageChannels[targetChannel - 1],
            diameterRange,
            localThresholdRadius,
            gaussianBlurSigma,
            shouldRemoveAxons
        )

        return CounterResult(cells.size, cells)
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

            val file: File = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE)
            val imp = IJ.openImage(file.path)
            imp.show()
            ij.command().run(RGCCounter::class.java, true)
        }
    }

    /** Displays the preview. */
    override fun preview() {
        if (preview) {
            val image = WindowManager.getCurrentImage()
            if (image == null) {
                MessageDialog(IJ.getInstance(), "Error", "There is no file open")
                return
            }
            val diameterRange: CellDiameterRange
            try {
                diameterRange = CellDiameterRange.parseFromText(cellDiameterText)
            } catch (e: DiameterParseException) {
                cancel()
                return
            }

            val result = try {
                process(image, diameterRange)
            } catch (e: ChannelDoesNotExistException) {
                cancel()
                return
            }

            image.show()
            logService.info(result.cells.size)
            drawCells(image, result.cells)
        }
    }

    /** Called when the preview box is unchecked. */
    override fun cancel() {
        val roiManager = RoiManager.getRoiManager()
        roiManager.reset()
        roiManager.close()
    }

    /** Called when the preview parameter value changes. */
    private fun previewChanged() {
        if (!preview) cancel()
    }
}
