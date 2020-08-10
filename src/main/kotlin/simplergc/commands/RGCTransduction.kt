package simplergc.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.plugin.frame.RoiManager
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
import simplergc.services.CellColocalizationService.CellAnalysis
import simplergc.services.CellDiameterRange
import simplergc.services.CellSegmentationService
import simplergc.services.DiameterParseException
import simplergc.services.Parameters
import simplergc.services.cellcomparator.SubsetPixelCellComparator
import simplergc.services.colocalizer.BucketedNaiveColocalizer
import simplergc.services.colocalizer.PositionedCell
import simplergc.services.colocalizer.addToRoiManager
import simplergc.services.colocalizer.drawCells
import simplergc.services.colocalizer.output.CsvColocalizationOutput
import simplergc.services.colocalizer.output.ImageJTableColocalizationOutput
import simplergc.services.colocalizer.output.XlsxColocalizationOutput
import simplergc.services.colocalizer.resetRoiManager
import simplergc.widgets.AlignedTextWidget
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

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
        label = "Select channels",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var selectChannelHeader: String

    /**
     * Specify the channel for the target cell. ImageJ does not have a way to retrieve
     * the channels available at the parameter initiation stage.
     * By default this is 1 (red) channel.
     */
    @Parameter(
        label = "Morphology channel",
        min = "1",
        stepSize = "1",
        required = true,
        persist = true
    )
    var targetChannel = 1

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
        label = "",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var emptyLineUnused1: String

    @Parameter(
        label = "Image processing parameters",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var preprocessingParamsHeader: String

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
        label = "Exclude axons in morphology channel",
        required = true,
        persist = true
    )
    var shouldRemoveAxonsFromTargetChannel: Boolean = false

    @Parameter(
        label = "Exclude axons in transduction channel",
        required = true,
        persist = true
    )
    var shouldRemoveAxonsFromTransductionChannel: Boolean = false

    @Parameter(
        label = "",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var emptyLineUnused2: String

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
        const val CSV = "Save as CSV files"
    }

    @Parameter(
        label = "Results output:",
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

    data class ChannelResult(val name: String, val cellAnalyses: List<CellAnalysis>) {
        val avgMorphologyArea = (cellAnalyses.sumBy { it.area } / cellAnalyses.size)
        val meanFluorescenceIntensity = (cellAnalyses.sumBy { it.mean } / cellAnalyses.size)
        val medianFluorescenceIntensity = (cellAnalyses.sumBy { it.median } / cellAnalyses.size)
        val minFluorescenceIntensity = (cellAnalyses.sumBy { it.min } / cellAnalyses.size)
        val maxFluorescenceIntensity = (cellAnalyses.sumBy { it.max } / cellAnalyses.size)
        val rawIntDen = (cellAnalyses.sumBy { it.rawIntDen } / cellAnalyses.size)
    }

    /**
     * Result of transduction analysis for output.
     * @property targetCellCount Number of red channel cells.
     * @property channelResults Quantification of each channel's cells overlapping target cells.
     * @property overlappingTwoChannelCells List of cells which overlap two channels.
     *
     */
    data class TransductionResult(
        val targetCellCount: Int, // Number of red cells
        val channelResults: List<ChannelResult>,
        val overlappingTwoChannelCells: List<PositionedCell>,
        val overlappingOverlaidCells: List<PositionedCell>
    ) {
        val transducedCellCount = overlappingTwoChannelCells.size
        val transductionEfficiency = DecimalFormat("#.##").format(
            (overlappingTwoChannelCells.size / targetCellCount.toDouble()) * 100).toDouble()
    }

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
        addToRoiManager(result.overlappingOverlaidCells)
    }

    private fun writeOutput(inputFileName: String, result: TransductionResult) {
        val transductionParameters = Parameters.Transduction(
            shouldRemoveAxonsFromTargetChannel,
            transducedChannel,
            shouldRemoveAxonsFromTransductionChannel,
            cellDiameterText,
            localThresholdRadius,
            gaussianBlurSigma,
            targetChannel
        )
        val output = when (outputFormat) {
            OutputFormat.DISPLAY -> ImageJTableColocalizationOutput(transductionParameters, result, uiService)
            OutputFormat.XLSX -> XlsxColocalizationOutput(outputFile!!, transductionParameters)
            OutputFormat.CSV -> CsvColocalizationOutput(outputFile!!, transductionParameters)
            else -> throw IllegalArgumentException("Invalid output type provided")
        }

        output.addTransductionResultForFile(result, inputFileName)

        try {
            output.output()
        } catch (ioe: IOException) {
            displayErrorDialog(ioe.message)
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
        val numChannels = image.nChannels
        if (targetChannel < 1 || targetChannel > numChannels) {
            throw ChannelDoesNotExistException(
                "Target channel selected ($targetChannel) does not exist. " +
                    "There are $numChannels channels available"
            )
        }
        if (transducedChannel < 1 || transducedChannel > image.nChannels) {
            throw ChannelDoesNotExistException(
                "Transduced channel selected ($transducedChannel) does not exist. " +
                    "There are $numChannels channels available"
            )
        }
        return analyseTransduction(image, targetChannel, transducedChannel, cellDiameterRange)
    }

    private fun analyseTransduction(
        image: ImagePlus,
        targetChannel: Int,
        transducedChannel: Int,
        cellDiameterRange: CellDiameterRange
    ): TransductionResult {
        logService.info("Starting extraction")

        // Extract relevant channels
        val channelImages = ChannelSplitter.split(image)
        val targetChannelImage = channelImages[targetChannel - 1]
        val transducedChannelImage = channelImages[transducedChannel - 1]

        statusService.showStatus(25, 100, "Extracting morphology cells...")

        val targetCells = cellSegmentationService.extractCells(
            targetChannelImage,
            cellDiameterRange,
            localThresholdRadius,
            gaussianBlurSigma,
            shouldRemoveAxonsFromTargetChannel
        )

        statusService.showStatus(25, 100, "Extracting transduced cells...")

        // Allow cells in the transduced channel to have unbounded area
        val transducedCells = filterCellsByIntensity(
            cellSegmentationService.extractCells(
                transducedChannelImage,
                CellDiameterRange(cellDiameterRange.smallest, Double.MAX_VALUE),
                localThresholdRadius,
                gaussianBlurSigma,
                shouldRemoveAxonsFromTransductionChannel
            ),
            transducedChannelImage
        )

        statusService.showStatus(80, 100, "Analysing transduction...")
        logService.info("Starting analysis")

        // Target layer is based and transduced layer is overlaid.
        val targetTransducedAnalysis = BucketedNaiveColocalizer(
            cellDiameterRange.largest.toInt(),
            targetChannelImage.width,
            targetChannelImage.height,
            SubsetPixelCellComparator(threshold = 0.5f)
        ).analyseColocalization(targetCells, transducedCells)

        // Measure intensity of all channels
        val rois = targetTransducedAnalysis.overlappingOverlaid.map { it.toRoi() }

        val channelResults = mutableListOf<ChannelResult>()
        val transductionChannelResult = ChannelResult(
            "Transduction",
            cellColocalizationService.analyseCellIntensity(
                channelImages[transducedChannel - 1],
                rois
            )
        )
        channelResults.add(transductionChannelResult)
        channelResults.add(
            ChannelResult(
                "Morphology", cellColocalizationService.analyseCellIntensity(
                    channelImages[targetChannel - 1],
                    rois
                )
            )
        )
        for (channel in (1..channelImages.size).toSet() - setOf(targetChannel, transducedChannel)) {
            val result = ChannelResult("Channel $channel",
                cellColocalizationService.analyseCellIntensity(
                    channelImages[channel - 1],
                    targetTransducedAnalysis.overlappingOverlaid.map { it.toRoi() }
                )
            )
            channelResults.add(result)
        }

        // We return the overlapping target channel instead of transduced channel as we want to mark the target layer,
        // not the transduced layer.
        return TransductionResult(
            targetCellCount = targetCells.size,
            channelResults = channelResults,
            overlappingTwoChannelCells = targetTransducedAnalysis.overlappingBase,
            overlappingOverlaidCells = targetTransducedAnalysis.overlappingOverlaid
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
