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

    // /***
    //  * Following Parameters allow the user to tune the plugin.
    //  */
    // @Parameter(
    //     label = "Preprocessing Parameters:",
    //     visibility = ItemVisibility.MESSAGE,
    //     required = false
    // )
    // private lateinit var preprocessingParamsHeader: String
    //
    // /**
    //  *  Decide whether we want to subtract the background or not.
    //  */
    // @Parameter(
    //     label = "Subtract Background?",
    //     required = true,
    //     persist = false
    // )
    // private var shouldSubtractBackground = true
    //
    // /**
    //  * Used during the cell identification stage to reduce overlapping cells
    //  * being grouped into a single cell.
    //  *
    //  * TODO(#5): Figure out what this value should be.
    //  */
    // @Parameter(
    //     label = "Largest Cell Diameter",
    //     min = "5.0",
    //     stepSize = "1.0",
    //     style = NumberWidget.SPINNER_STYLE,
    //     required = true,
    //     persist = false
    // )
    // private var largestCellDiameter = 30.0
    //
    object ThresholdTypes {
        const val GLOBAL = "Global"
        const val LOCAL = "Local"
    }
    //
    // /**
    //  * Decide on global/local Threshold.
    //  */
    // @Parameter(
    //     label = "Threshold type",
    //     style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE,
    //     choices = [ThresholdTypes.GLOBAL, ThresholdTypes.LOCAL],
    //     required = true,
    //     persist = false
    // )
    // private var thresholdLocality = ThresholdTypes.GLOBAL
    //
    object GlobalThresholdAlgos {
        const val OTSU = "Otsu"
        const val MOMENTS = "Moments"
        const val SHANBHAG = "Shanbhag"
    }
    //
    // /**
    //  * Decide on Thresholding Algorithm.
    //  */
    // @Parameter(
    //     label = "Global Thresholding Algorithm",
    //     style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE,
    //     choices = [GlobalThresholdAlgos.OTSU, GlobalThresholdAlgos.MOMENTS, GlobalThresholdAlgos.SHANBHAG],
    //     required = true,
    //     persist = false
    // )
    // private var globalThresholdAlgo = GlobalThresholdAlgos.OTSU
    //
    object LocalThresholdAlgos {
        const val OTSU = "Otsu"
        const val BERNSEN = "Bernsen"
        const val NIBLACK = "Niblack"
    }
    //
    // /**
    //  * Decide on Thresholding Algorithm.
    //  */
    // @Parameter(
    //     label = "Local Thresholding Algorithm",
    //     style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE,
    //     choices = [LocalThresholdAlgos.OTSU, LocalThresholdAlgos.BERNSEN, LocalThresholdAlgos.NIBLACK],
    //     required = true,
    //     persist = false
    // )
    // private var localThresholdAlgo = LocalThresholdAlgos.OTSU
    //
    // /**
    //  * Decide on local Threshold radius.
    //  */
    // @Parameter(
    //     label = "Local Threshold Radius",
    //     min = "0.0",
    //     stepSize = "1.0",
    //     style = NumberWidget.SPINNER_STYLE,
    //     required = true,
    //     persist = false
    // )
    // private var localThresholdRadius = 30
    //
    // /**
    //  *  Decide whether we want to try and despeckle the image.
    //  */
    // @Parameter(
    //     label = "Despeckle?",
    //     required = true,
    //     persist = false
    // )
    // private var shouldDespeckle = true
    //
    // /**
    //  * Select filter radius for median filter when despeckling.
    //  */
    // @Parameter(
    //     label = "Despeckle Radius",
    //     min = "0.0",
    //     stepSize = "0.5",
    //     style = NumberWidget.SPINNER_STYLE,
    //     required = true,
    //     persist = false
    // )
    // private var despeckleRadius = 1.0
    //
    // /**
    //  *  Decide whether we want to apply Gaussian Blur.
    //  */
    // @Parameter(
    //     label = "Gaussian Blur?",
    //     required = true,
    //     persist = false
    // )
    // private var shouldGaussianBlur = true
    //
    // /**
    //  * Applied to the input image to reduce sensitivity of the thresholding
    //  * algorithm. Higher value means more blur.
    //  */
    // @Parameter(
    //     label = "Gaussian Blur Sigma (Radius)",
    //     description = "Reduces sensitivity to cell edges by blurring the " +
    //         "overall image. Higher is less sensitive.",
    //     min = "0.0",
    //     stepSize = "1.0",
    //     style = NumberWidget.SPINNER_STYLE,
    //     required = true,
    //     persist = false
    // )
    // private var gaussianBlurSigma = 3.0

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
        var params = PreprocessingParameters()
        val paramsDialog = GenericDialog("Tune Parameters")
        paramsDialog.addCheckbox("Subtract Background?", params.shouldSubtractBackground)
        paramsDialog.addNumericField("Largest Cell Diameter", params.largestCellDiameter, 3)
        paramsDialog.addChoice("Threshold Locality", arrayOf(ThresholdTypes.GLOBAL, ThresholdTypes.LOCAL), params.thresholdLocality)
        paramsDialog.addChoice("Global Thresholding Algorithm", arrayOf(GlobalThresholdAlgos.OTSU, GlobalThresholdAlgos.MOMENTS, GlobalThresholdAlgos.SHANBHAG), params.globalThresholdAlgo)
        paramsDialog.addChoice("Local Thresholding Algorithm", arrayOf(LocalThresholdAlgos.OTSU, LocalThresholdAlgos.BERNSEN, LocalThresholdAlgos.NIBLACK), params.localThresholdAlgo)
        paramsDialog.addNumericField("Local Threshold radius", params.localThresholdRadius.toDouble(), 3)
        paramsDialog.addCheckbox("Despeckle?", params.shouldDespeckle)
        paramsDialog.addNumericField("Despeckle Radius", params.despeckleRadius, 3)
        paramsDialog.addCheckbox("Gaussian Blur?", params.shouldGaussianBlur)
        paramsDialog.addNumericField("Gaussian Blur Sigma", params.gaussianBlurSigma, 3)
        paramsDialog.showDialog()
        if (paramsDialog.wasCanceled()) return params;
        // TODO: (Tiger) Set parameters based on dialog values
        return params
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
