package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ZProjector
import java.io.File
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
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.preprocessing.tuneParameters
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.colocalizer.PositionedCell
import simplecolocalization.services.colocalizer.addToRoiManager
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.ImageJTableCounterOutput
import simplecolocalization.services.counter.output.XMLCounterOutput
import java.io.IOException
import javax.xml.transform.TransformerException

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
        label = "Manually Tune Parameters?",
        required = true,
        persist = false
    )
    private var tuneParams = false

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
        label = "Output Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var outputParametersHeader: String

    /**
     * The user can optionally output the results to a file.
     */
    object OutputDestination {
        const val DISPLAY = "Display in table"
        const val CSV = "Save as CSV file"
        const val XML = "Save as XML file"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputDestination.DISPLAY, OutputDestination.CSV],
        required = true,
        persist = false,
        style = "radioButtonVertical"
    )
    private var outputDestination = OutputDestination.DISPLAY

    private var outputFile: File? = null

    /** Runs after the parameters above are populated. */
    override fun run() {
        var image = WindowManager.getCurrentImage()
        if (image != null) {
            if (image.nSlices > 1) {
                // Flatten slices of the image. This step should probably be done during the preprocessing step - however
                // this operation is not done in-place but creates a new image, which makes this hard.
                image = ZProjector.run(image, "max")
            }

            process(image)
        } else {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
        }
    }

    /** Processes single image. */
    private fun process(image: ImagePlus) {
        if (outputDestination != OutputDestination.DISPLAY && outputFile == null) {
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

        val preprocessingParams = if (tuneParams) {
                tuneParameters(largestCellDiameter) ?: return
            } else {
                PreprocessingParameters(largestCellDiameter = largestCellDiameter)
            }

        val path = image.originalFileInfo.directory + image.originalFileInfo.fileName
        val cells = countCells(path, preprocessingParams)

        displayOutput(cells.size, image.title)

        // The colocalization results are clearly displayed if the output
        // destination is set to DISPLAY, however, a visual confirmation
        // is useful if the output is saved to file.
        if (outputDestination != OutputDestination.DISPLAY) {
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The cell counting results have successfully been saved to the specified file."
            )
        }

        image.show()
        addToRoiManager(cells)
    }

    fun countCells(imagePath: String, preprocessingParameters: PreprocessingParameters): List<PositionedCell> {
        val image = ImagePlus(imagePath).duplicate()

        cellSegmentationService.preprocessImage(image, preprocessingParameters)
        cellSegmentationService.segmentImage(image)

        return cellSegmentationService.identifyCells(image)
    }

    private fun displayOutput(numCells: Int, file: String) {

        val output = when (outputDestination) {
            OutputDestination.DISPLAY -> ImageJTableCounterOutput(uiService)
            OutputDestination.CSV -> CSVCounterOutput(outputFile!!)
            OutputDestination.XML -> XMLCounterOutput(outputFile!!)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        output.addCountForFile(numCells, file)

        try {
            output.output()
        } catch (te: TransformerException) {
            displayErrorDialog("XML")
        } catch (ioe: IOException) {
            displayErrorDialog("")
        }
    }

    private fun displayErrorDialog(fileTypeError: String) {
        GenericDialog("Error").apply {
            addMessage("Unable to save results to "+ fileTypeError +" file. Ensure the output file is not currently open by other programs and try again.")
            hideCancelButton()
            showDialog()
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

            val file: File = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE)
            val imp = IJ.openImage(file.path)
            imp.show()
            ij.command().run(SimpleCellCounter::class.java, true)
        }
    }
}
