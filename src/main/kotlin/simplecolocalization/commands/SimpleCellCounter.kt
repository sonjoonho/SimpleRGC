package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.MessageDialog
import ij.plugin.ZProjector
import ij.plugin.frame.RoiManager
import java.io.File
import net.imagej.Dataset
import net.imagej.ImageJ
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.preprocessing.tuneParameters
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.counter.output.CSVCounterOutput
import simplecolocalization.services.counter.output.ImageJTableCounterOutput

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
        label = "Output File (if saving):",
        style = "save",
        required = false
    )
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
        if (outputDestination != SimpleColocalization.OutputDestination.DISPLAY && outputFile == null) {
            MessageDialog(
                IJ.getInstance(),
                "Error", "File to save to not specified."
            )
            return
        }

        // We need to create a copy of the image since we want to show the results on the original image, but
        // preprocessing is done in-place which changes the image.
        val originalImage = image.duplicate()
        originalImage.title = "${image.title} - segmented"

        val preprocessingParams = if (tuneParams) tuneParameters() else PreprocessingParameters()

        cellSegmentationService.preprocessImage(image, preprocessingParams)
        cellSegmentationService.segmentImage(image)

        val roiManager = RoiManager.getRoiManager()
        val cells = cellSegmentationService.identifyCells(roiManager, image)
        cellSegmentationService.markCells(originalImage, cells)

        if (outputDestination == OutputDestination.DISPLAY) {
            ImageJTableCounterOutput(cells.size, uiService).output()
        } else if (outputDestination == OutputDestination.CSV) {
            CSVCounterOutput(cells.size, outputFile!!).output()
        }

        // The colocalization results are clearly displayed if the output
        // destination is set to DISPLAY, however, a visual confirmation
        // is useful if the output is saved to file.
        if (outputDestination != SimpleColocalization.OutputDestination.DISPLAY) {
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The cell counting results have successfully been saved to the specified file."
            )
        }

        originalImage.show()
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

            val file: File = ij.ui().chooseFile(null, "open")
            val dataset: Dataset = ij.scifio().datasetIO().open(file.path)

            ij.ui().show(dataset)
            ij.command().run(SimpleCellCounter::class.java, true)
        }
    }
}
