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
import kotlin.math.roundToInt

data class PreprocessingParameters(
    val shouldSubtractBackground: Boolean = true,
    val largestCellDiameter: Double = 30.0,
    val thresholdLocality: String = SimpleCellCounter.ThresholdTypes.GLOBAL,
    val globalThresholdAlgo: String = SimpleCellCounter.GlobalThresholdAlgos.OTSU,
    val localThresholdAlgo: String = SimpleCellCounter.LocalThresholdAlgos.OTSU,
    val localThresholdRadius: Int = 15,
    val shouldDespeckle: Boolean = true,
    val despeckleRadius: Double = 1.0,
    val shouldGaussianBlur: Boolean = true,
    val gaussianBlurSigma: Double = 3.0) {
}

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
        label = "Tune Parameters?",
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

    private fun tuneParameters() : PreprocessingParameters {
        var defaultParams = PreprocessingParameters()
        // TODO: (Tiger) refactor into separate functions, renderDialog, getParamsfromDialog
        val paramsDialog = GenericDialog("Tune Parameters")
        paramsDialog.addCheckbox("Subtract Background?", defaultParams.shouldSubtractBackground)
        paramsDialog.addNumericField("Largest Cell Diameter", defaultParams.largestCellDiameter, 0)
        paramsDialog.addChoice("Threshold Locality", arrayOf(ThresholdTypes.GLOBAL, ThresholdTypes.LOCAL), defaultParams.thresholdLocality)
        paramsDialog.addChoice("Global Thresholding Algorithm", arrayOf(GlobalThresholdAlgos.OTSU, GlobalThresholdAlgos.MOMENTS, GlobalThresholdAlgos.SHANBHAG), defaultParams.globalThresholdAlgo)
        paramsDialog.addChoice("Local Thresholding Algorithm", arrayOf(LocalThresholdAlgos.OTSU, LocalThresholdAlgos.BERNSEN, LocalThresholdAlgos.NIBLACK), defaultParams.localThresholdAlgo)
        paramsDialog.addNumericField("Local Threshold radius", defaultParams.localThresholdRadius.toDouble(), 0)
        paramsDialog.addCheckbox("Despeckle?", defaultParams.shouldDespeckle)
        paramsDialog.addNumericField("Despeckle Radius", defaultParams.despeckleRadius, 0)
        paramsDialog.addCheckbox("Gaussian Blur?", defaultParams.shouldGaussianBlur)
        paramsDialog.addNumericField("Gaussian Blur Sigma", defaultParams.gaussianBlurSigma, 0)
        paramsDialog.showDialog()
        if (paramsDialog.wasCanceled()) return defaultParams
        val shouldSubtractBackground = paramsDialog.nextBoolean
        val largestCellDiameter = paramsDialog.nextNumber
        val thresholdLocality = paramsDialog.nextChoice
        val globalThresholdAlgo = paramsDialog.nextChoice
        val localThresholdAlgo = paramsDialog.nextChoice
        val localThresholdRadius = paramsDialog.nextNumber.roundToInt()
        val shouldDespeckle = paramsDialog.nextBoolean
        val despeckleRadius = paramsDialog.nextNumber
        val shouldGaussianBlur = paramsDialog.nextBoolean
        val gaussianBlurSigma = paramsDialog.nextNumber
        return PreprocessingParameters(shouldSubtractBackground, largestCellDiameter, thresholdLocality, globalThresholdAlgo, localThresholdAlgo, localThresholdRadius, shouldDespeckle, despeckleRadius, shouldGaussianBlur, gaussianBlurSigma)
    }

    /** Processes single image. */
    private fun process(image: ImagePlus) {
        // We need to create a copy of the image since we want to show the results on the original image, but
        // preprocessing is done in-place which changes the image.
        val originalImage = image.duplicate()
        originalImage.title = "${image.title} - segmented"

        val cellSegmentationService = CellSegmentationService()

        val preprocessingParams = if (tuneParams) tuneParameters() else PreprocessingParameters()

        cellSegmentationService.preprocessImage(image, preprocessingParams)
        cellSegmentationService.segmentImage(image)

        val roiManager = RoiManager.getRoiManager()
        val cells = cellSegmentationService.identifyCells(roiManager, image)
        // roiManager.runCommand("Delete")
        // Line for selecrion of cells (Doesn't do much)
        // cellSegmentationService.markCells(originalImage, cells)

        originalImage.show()

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

            ij.launch()

            val file: File = ij.ui().chooseFile(null, "open")
            val dataset: Dataset = ij.scifio().datasetIO().open(file.path)

            ij.ui().show(dataset)
            ij.command().run(SimpleCellCounter::class.java, true)
        }
    }
}
