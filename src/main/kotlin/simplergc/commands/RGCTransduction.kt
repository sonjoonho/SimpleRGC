package simplergc.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.HistogramWindow
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.plugin.frame.RoiManager
import ij.process.FloatProcessor
import ij.process.StackStatistics
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import net.imagej.ImageJ
import net.imagej.ops.OpService
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
import simplergc.services.CellColocalizationService
import simplergc.services.CellDiameterRange
import simplergc.services.CellSegmentationService
import simplergc.services.DiameterParseException
import simplergc.services.cellcomparator.SubsetPixelCellComparator
import simplergc.services.colocalizer.BucketedNaiveColocalizer
import simplergc.services.colocalizer.PositionedCell
import simplergc.services.colocalizer.addToRoiManager
import simplergc.services.colocalizer.drawCells
import simplergc.services.colocalizer.output.CSVColocalizationOutput
import simplergc.services.colocalizer.output.ImageJTableColocalizationOutput
import simplergc.services.colocalizer.output.XLSXColocalizationOutput
import simplergc.services.colocalizer.resetRoiManager
import simplergc.widgets.AlignedTextWidget

@Plugin(type = Command::class, menuPath = "Plugins > Simple RGC > RGC Transduction")
class RGCTransduction : Command, Previewable {

    private val intensityPercentageThreshold: Float = 90f

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var statusService: StatusService

    @Parameter
    private lateinit var cellSegmentationService: CellSegmentationService

    @Parameter
    private lateinit var cellColocalizationService: CellColocalizationService

    @Parameter
    private lateinit var opsService: OpService

    /**
     * Entry point for UI operations, automatically handling both graphical and
     * headless use of this plugin.
     */
    @Parameter
    private lateinit var uiService: UIService

    @Parameter(
        label = "Channel selection",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var channelSelectionHeader: String

    /**
     * Specify the channel for the target cell. ImageJ does not have a way to retrieve
     * the channels available at the parameter initiation stage.
     * By default this is 1 (red) channel.
     */
    @Parameter(
        label = "Cell morphology channel",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    var targetChannel = 1

    @Parameter(
        label = "Exclude axons from cell morphology channel",
        required = true,
        persist = true
    )
    var shouldRemoveAxonsFromTargetChannel: Boolean = false

    /**
     * Specify the channel for the transduced cells.
     * By default this is the 2 (green) channel.
     */
    @Parameter(
        label = "Transduction channel",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    var transducedChannel = 2

    @Parameter(
        label = "Exclude axons from transduction channel",
        required = true,
        persist = true
    )
    var shouldRemoveAxonsFromTransductionChannel: Boolean = false

    @Parameter(
        label = "Preprocessing parameters",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var preprocessingParamsHeader: String

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
    var localThresholdRadius = 30

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
        const val XLSX = "Save as Excel file (Recommended)"
        const val CSV = "Save as CSV files"
    }

    @Parameter(
        label = "Results Output:",
        choices = [OutputFormat.DISPLAY, OutputFormat.XLSX, OutputFormat.CSV],
        required = true,
        persist = true,
        style = "radioButtonVertical"
    )
    private var outputFormat = OutputFormat.DISPLAY

    @Parameter(
        label = "Output file (if saving):",
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

    /**
     * Result of transduction analysis for output.
     * @property targetCellCount Number of red channel cells.
     * @property overlappingTransducedIntensityAnalysis Quantification of each transduced cell overlapping target cells.
     * @property overlappingTwoChannelCells List of cells which overlap two channels.
     *
     */
    data class TransductionResult(
        val targetCellCount: Int, // Number of red cells
        val overlappingTransducedIntensityAnalysis: Array<CellColocalizationService.CellAnalysis>,
        val overlappingTwoChannelCells: List<PositionedCell>
    )

    /**
     * Class that consolidates all the parameters required for transduction
     * Used to pass respective values to the the CSV output
     */
    data class TransductionParameters(
        val excludeAxonsFromMorphologyChannel: String,
        val transductionChannel: String,
        val excludeAxonsFromTransductionChannel: String,
        val cellDiameterRange: String,
        val localThresholdRadius: String,
        val gaussianBlurSigma: String,
        val morphologyChannel: String,
        val outputFile: File
    )

    override fun run() {
        val image = WindowManager.getCurrentImage()
        if (image == null) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }

        val cellDiameterRange: CellDiameterRange
        try {
            cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterText)
        } catch (e: DiameterParseException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
            return
        }

        // TODO(#135): Remove duplication in this code fragment.
        if (outputFormat != OutputFormat.DISPLAY && outputFile == null) {
            val path = image.originalFileInfo.directory
            val name = FilenameUtils.removeExtension(image.originalFileInfo.fileName)
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
            process(image, cellDiameterRange)
        } catch (e: ChannelDoesNotExistException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
            return
        }

        statusService.showStatus(100, 100, "Done!")
        writeOutput(image.originalFileInfo.fileName, result)

        image.show()
        addToRoiManager(result.overlappingTwoChannelCells)
        // showHistogram(result.overlappingTransducedIntensityAnalysis)
    }

    private fun writeOutput(inputFileName: String, result: TransductionResult) {

        val transductionParameters = TransductionParameters(
            this.shouldRemoveAxonsFromTargetChannel.toString(),
            this.transducedChannel.toString(),
            this.shouldRemoveAxonsFromTransductionChannel.toString(),
            this.cellDiameterText,
            this.localThresholdRadius.toString(),
            this.gaussianBlurSigma.toString(),
            this.targetChannel.toString(),
            this.outputFile!!
        )

        val output = when (outputFormat) {
            OutputFormat.DISPLAY -> ImageJTableColocalizationOutput(result, uiService)
            OutputFormat.XLSX -> XLSXColocalizationOutput(transductionParameters)
            OutputFormat.CSV -> CSVColocalizationOutput(transductionParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        output.addTransductionResultForFile(result, inputFileName)

        try {
            output.output()
        } catch (ioe: IOException) {
            displayOutputFileErrorDialog()
        }

        // The colocalization results are clearly displayed if the output
        // destination is set to DISPLAY, however, a visual confirmation
        // is useful if the output is saved to file.
        if (outputFormat != OutputFormat.DISPLAY) {
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The colocalization results have successfully been saved to the specified file"
            )
        }
    }

    /** Processes single image. */
    @Throws(ChannelDoesNotExistException::class)
    fun process(
        image: ImagePlus,
        cellDiameterRange: CellDiameterRange
    ): TransductionResult {
        val imageChannels = ChannelSplitter.split(image)
        if (targetChannel < 1 || targetChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Target channel selected ($targetChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        if (transducedChannel < 1 || transducedChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Transduced channel selected ($transducedChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        return analyseTransduction(
            imageChannels[targetChannel - 1],
            imageChannels[transducedChannel - 1],
            cellDiameterRange
        )
    }

    fun analyseTransduction(
        targetChannel: ImagePlus,
        transducedChannel: ImagePlus,
        cellDiameterRange: CellDiameterRange
    ): TransductionResult {
        logService.info("Starting extraction")
        val targetCells = cellSegmentationService.extractCells(
            targetChannel,
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma,
            shouldRemoveAxonsFromTargetChannel
        )

        // Allow cells in the transduced channel to have unbounded area
        val transducedCells = filterCellsByIntensity(
            cellSegmentationService.extractCells(
                transducedChannel,
                CellDiameterRange(cellDiameterRange.smallest, Double.MAX_VALUE),
                localThresholdRadius,
                gaussianBlurSigma,
                shouldRemoveAxonsFromTransductionChannel
            ),
            transducedChannel
        )

        statusService.showStatus(80, 100, "Analysing transduction...")
        logService.info("Starting analysis")

        // Target layer is based and transduced layer is overlaid.
        val targetTransducedAnalysis = BucketedNaiveColocalizer(
            cellDiameterRange.largest.toInt(),
            targetChannel.width,
            targetChannel.height,
            SubsetPixelCellComparator(threshold = 0.5f)
        ).analyseColocalization(targetCells, transducedCells)

        val transductionIntensityAnalysis = cellColocalizationService.analyseCellIntensity(
            transducedChannel,
            targetTransducedAnalysis.overlappingOverlaid.map { it.toRoi() }.toTypedArray()
        )

        // We return the overlapping target channel instead of transduced channel as we want to mark the target layer,
        // not the transduced layer.
        return TransductionResult(
            targetCellCount = targetCells.size,
            overlappingTransducedIntensityAnalysis = transductionIntensityAnalysis,
            overlappingTwoChannelCells = targetTransducedAnalysis.overlappingBase
        )
    }

    /**
     * Filter the position cells by their average intensity in the given image
     * using the intensityPercentageThreshold.
     */
    private fun filterCellsByIntensity(cells: List<PositionedCell>, image: ImagePlus): List<PositionedCell> {
        var maxIntensity = 0.0f
        var minIntensity = Float.MAX_VALUE
        val intensities = cells.map {
            val intensity = it.getMeanIntensity(image)
            maxIntensity = max(maxIntensity, intensity)
            minIntensity = min(minIntensity, intensity)
            intensity
        }
        val threshold = maxIntensity - (maxIntensity - minIntensity) * (intensityPercentageThreshold / 100)
        val thresholdedCells = mutableListOf<PositionedCell>()
        intensities.forEachIndexed { index, intensity ->
            if (intensity > threshold) {
                thresholdedCells.add(cells[index])
            }
        }
        return thresholdedCells
    }

    /**
     * Displays the resulting colocalization results as a histogram.
     */
    private fun showHistogram(analysis: Array<CellColocalizationService.CellAnalysis>) {
        val data = analysis.map { it.median.toFloat() }.toFloatArray()
        val ip = FloatProcessor(analysis.size, 1, data, null)
        val imp = ImagePlus("", ip)
        val stats = StackStatistics(imp, 256, 0.0, 256.0)
        var maxCount = 0
        for (i in stats.histogram.indices) {
            if (stats.histogram[i] > maxCount)
                maxCount = stats.histogram[i]
        }
        stats.histYMax = maxCount
        HistogramWindow("Median intensity distribution of transduced cells overlapping target cells", imp, stats)
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
            ij.context().inject(CellColocalizationService())
            ij.launch()

            val file: File = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE)
            val imp = IJ.openImage(file.path)
            imp.show()
            ij.command().run(RGCTransduction::class.java, true)
        }
    }

    override fun preview() {
        if (preview) {
            val image = WindowManager.getCurrentImage()
            if (image == null) {
                MessageDialog(IJ.getInstance(), "Error", "There is no file open")
                return
            }

            val cellDiameterRange: CellDiameterRange
            try {
                cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterText)
            } catch (e: DiameterParseException) {
                cancel()
                return
            }

            val result = try {
                process(image, cellDiameterRange)
            } catch (e: ChannelDoesNotExistException) {
                cancel()
                return
            }

            image.show()
            drawCells(image, result.overlappingTwoChannelCells)
        }
    }

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

class ChannelDoesNotExistException(message: String) : Exception(message)
