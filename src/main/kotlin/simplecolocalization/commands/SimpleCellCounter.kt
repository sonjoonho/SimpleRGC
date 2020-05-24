package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import java.io.File
import java.io.IOException
import javax.xml.transform.TransformerException
import net.imagej.ImageJ
import org.apache.commons.io.FilenameUtils
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.FileWidget
import org.scijava.widget.NumberWidget
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.colocalizer.PositionedCell
import simplecolocalization.services.colocalizer.addToRoiManager
import simplecolocalization.services.colocalizer.resetRoiManager
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.ImageJTableCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput

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
@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Cell Counter")
class SimpleCellCounter : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var cellSegmentationService: CellSegmentationService

    /**
     * Entry point for UI operations, automatically handling both graphical and
     * headless use of this plugin.
     */
    @Parameter
    private lateinit var uiService: UIService

    @Parameter(
        label = "Select Channel To Use:",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    var targetChannel = 1

    @Parameter(
        label = "Image Processing Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var processingParametersHeader: String

    /**
     * Used during the cell identification stage to filter out cells that are too small
     */
    @Parameter(
        label = "Smallest Cell Diameter (px):",
        description = "Used as minimum diameter when identifying cells",
        min = "0.0",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    var smallestCellDiameter = 0.0

    /**
     * Used during the cell segmentation stage to perform local thresholding or
     * background subtraction.
     */
    @Parameter(
        label = "Largest Cell Diameter (px):",
        description = "Used to apply the rolling ball algorithm to subtract " +
            "the background when thresholding",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    var largestCellDiameter = 30.0

    /**
     * Used as the size of the window over which the threshold will be locally computed.
     */
    @Parameter(
        label = "Local Threshold Radius",
        // TODO: Improve this description to make more intuitive.
        description = "The radius of the local domain over which the threshold will be computed.",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    var localThresholdRadius = 20

    @Parameter(
        label = "Gaussian Blur Sigma:",
        description = "Sigma value used for blurring the image during the processing," +
            " a lower value is recommended if there are lots of cells densely packed together",
        min = "1",
        stepSize = "1",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    var gaussianBlurSigma = 3.0

    @Parameter(
            label = "Remove Axons",
            required = true,
            persist = false
    )
    private var shouldRemoveAxons: Boolean = false

    @Parameter(
        label = "Output Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var outputParametersHeader: String

    /**
     * The user can optionally output the results to a file.
     */
    object OutputFormat {
        const val DISPLAY = "Display in ImageJ"
        const val CSV = "Save as CSV file"
        const val XML = "Save as XML file"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputFormat.DISPLAY, OutputFormat.CSV, OutputFormat.XML],
        required = true,
        persist = false,
        style = "radioButtonVertical"
    )
    private var outputFormat = OutputFormat.DISPLAY

    @Parameter(
        label = "Output File (if saving):",
        style = "save",
        required = false
    )
    private var outputFile: File? = null

    data class CounterResult(val count: Int, val cells: List<PositionedCell>)

    /** Runs after the parameters above are populated. */
    override fun run() {
        val image = WindowManager.getCurrentImage()
        if (image == null) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }

        if (smallestCellDiameter > largestCellDiameter) {
            MessageDialog(
                IJ.getInstance(),
                "Error",
                "Smallest cell diameter must be smaller than the largest cell diameter"
            )
            return
        }

        if (outputFormat != OutputFormat.DISPLAY && outputFile == null) {
            val path = image.originalFileInfo.directory
            val name = FilenameUtils.removeExtension(image.originalFileInfo.fileName) + ".csv"
            outputFile = File(path + name)
            if (!outputFile!!.createNewFile()) {
                val dialog = GenericDialog("Warning")
                dialog.addMessage("Overwriting file \"$name\"")
                dialog.showDialog()
                if (dialog.wasCanceled()) return
            }
        }

        resetRoiManager()

        val result = process(image)

        writeOutput(result.count, image.title)

        image.show()
        addToRoiManager(result.cells)
    }

    private fun writeOutput(numCells: Int, file: String) {
        val output = when (outputFormat) {
            OutputFormat.DISPLAY -> ImageJTableCounterOutput(uiService)
            OutputFormat.CSV -> CSVCounterOutput(outputFile!!)
            OutputFormat.XML -> XMLCounterOutput(outputFile!!)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        output.addCountForFile(numCells, file)

        try {
            output.output()
        } catch (te: TransformerException) {
            displayOutputFileErrorDialog(filetype = "XML")
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
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
    fun process(image: ImagePlus): CounterResult {
        val imageChannels = ChannelSplitter.split(image)
        if (targetChannel < 1 || targetChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Target channel selected ($targetChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        val cells = cellSegmentationService.extractCells(imageChannels[targetChannel - 1], smallestCellDiameter, largestCellDiameter, localThresholdRadius, gaussianBlurSigma, shouldRemoveAxons)
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
            ij.command().run(SimpleCellCounter::class.java, true)
        }
    }
}
