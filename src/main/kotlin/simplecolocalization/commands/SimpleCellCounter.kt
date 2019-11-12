package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ZProjector
import ij.plugin.frame.RoiManager
import java.io.File
import net.imagej.Dataset
import net.imagej.ImageJ
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.utils.PreprocessingParameters
import simplecolocalization.utils.tuneParameters

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

    object ThresholdTypes {
        const val GLOBAL = "Global"
        const val LOCAL = "Local"
    }

    object GlobalThresholdAlgos {
        const val OTSU = "Otsu"
        const val MOMENTS = "Moments"
        const val SHANBHAG = "Shanbhag"
    }

    object LocalThresholdAlgos {
        const val OTSU = "Otsu"
        const val BERNSEN = "Bernsen"
        const val NIBLACK = "Niblack"
    }

    @Parameter(
        label = "Manually Tune Parameters?",
        required = true,
        persist = false
    )
    private var tuneParams = false

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
        // We need to create a copy of the image since we want to show the results on the original image, but
        // preprocessing is done in-place which changes the image.
        val originalImage = image.duplicate()
        originalImage.title = "${image.title} - segmented"

        val preprocessingParams = if (tuneParams) tuneParameters() else PreprocessingParameters()

        cellSegmentationService.preprocessImage(image, preprocessingParams)
        cellSegmentationService.segmentImage(image)

        val roiManager = RoiManager.getRoiManager()
        val cells = cellSegmentationService.identifyCells(roiManager, image)

        roiManager.reset()
        roiManager.close()
        image.hide()

        val cellCount = cells.size

        val countDialog = GenericDialog("Cell count")
        countDialog.addMessage("The cell counter counted " + cellCount + " cells.")
        countDialog.showDialog()
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
