package simplecolocalization.commands

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.HistogramWindow
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.process.FloatProcessor
import ij.process.StackStatistics
import java.io.File
import kotlin.math.max
import kotlin.math.min
import net.imagej.ImageJ
import net.imagej.ops.OpService
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
import simplecolocalization.services.CellColocalizationService
import simplecolocalization.services.CellSegmentationService
import simplecolocalization.services.cellcomparator.PixelCellComparator
import simplecolocalization.services.colocalizer.BucketedNaiveColocalizer
import simplecolocalization.services.colocalizer.PositionedCell
import simplecolocalization.services.colocalizer.addToRoiManager
import simplecolocalization.services.colocalizer.output.CSVColocalizationOutput
import simplecolocalization.services.colocalizer.output.ImageJTableColocalizationOutput

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Colocalization")
class SimpleColocalization : Command {

    private val intensityPercentageThreshold: Float = 90f

    @Parameter
    private lateinit var logService: LogService

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
        label = "Manually Tune Parameters?",
        required = true,
        persist = false
    )
    private var tuneParams = false
    // TODO(#84): Unimplemented!

    /**
     * Specify the channel for the target cell. ImageJ does not have a way to retrieve
     * the channels available at the parameter initiation stage.
     * By default this is 1 (red) channel.
     */
    @Parameter(
        label = "Target Cell Channel:",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    var targetChannel = 1

    /**
     * Specify the channel for the transduced cells.
     * By default this is the 2 (green) channel.
     */
    @Parameter(
        label = "Transduced Cell Channel:",
        min = "1",
        stepSize = "1",
        required = true,
        persist = false
    )
    var transducedChannel = 2

    /**
     * Specify the channel for the all cells channel.
     * By default this is the 0 (disabled).
     */
    @Parameter(
        label = "All Cells Channel (0 to disable):",
        min = "0",
        stepSize = "1",
        required = true,
        persist = false
    )
    var allCellsChannel = 0

    private fun isAllCellsEnabled(): Boolean {
        return allCellsChannel > 0
    }

    @Parameter(
        label = "Preprocessing Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var preprocessingParamsHeader: String

    /**
     * Used during the cell segmentation stage to reduce overlapping cells
     * being grouped into a single cell and perform local thresholding or
     * background subtraction.
     */
    @Parameter(
        label = "Largest Cell Diameter",
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

    @Parameter(
        label = "Output File (if saving):",
        style = "save",
        required = false
    )
    private var outputFile: File? = null

    /**
     * Result of transduction analysis for output.
     * @property targetCellCount Number of red channel cells.
     * @property overlappingTransducedCellAnalyses Quantification of each transduced cell overlapping target cells.
     * @property overlappingTwoChannelCells List of cells which overlap two channels.
     * @property overlappingThreeChannelCells List of cells which overlap three channels. null if not applicable.
     *
     * TODO(tc): Discuss whether we want to use targetCellCount in the single colocalisation plugin
     */
    data class TransductionResult(
        val targetCellCount: Int, // Number of red cells
        val overlappingTransducedCellAnalyses: Array<CellColocalizationService.CellAnalysis>,
        val overlappingTwoChannelCells: List<PositionedCell>,
        val overlappingThreeChannelCells: List<PositionedCell>?
    )

    override fun run() {
        val image = WindowManager.getCurrentImage()
        if (image == null) {
            MessageDialog(IJ.getInstance(), "Error", "There is no file open")
            return
        }

        // TODO(sonjoonho): Remove duplication in this code fragment.
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

        val result = try {
            process(image)
        } catch (e: ChannelDoesNotExistException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
            return
        }

        writeOutput(result)

        image.show()
        addToRoiManager(result.overlappingTwoChannelCells)
        showHistogram(result.overlappingTransducedCellAnalyses)
    }

    private fun writeOutput(result: TransductionResult) {
        if (outputDestination == OutputDestination.DISPLAY) {
            ImageJTableColocalizationOutput(result, uiService).output()
        } else if (outputDestination == OutputDestination.CSV) {
            CSVColocalizationOutput(result, outputFile!!).output()
        }

        // The colocalization results are clearly displayed if the output
        // destination is set to DISPLAY, however, a visual confirmation
        // is useful if the output is saved to file.
        if (outputDestination != OutputDestination.DISPLAY) {
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The colocalization results have successfully been saved to the specified file."
            )
        }
    }

    /** Processes single image. */
    @Throws(ChannelDoesNotExistException::class)
    fun process(image: ImagePlus): TransductionResult {
        val imageChannels = ChannelSplitter.split(image)
        if (targetChannel < 1 || targetChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Target channel selected ($targetChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        if (transducedChannel < 1 || transducedChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("Transduced channel selected ($transducedChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        if (isAllCellsEnabled() && allCellsChannel > imageChannels.size) {
            throw ChannelDoesNotExistException("All cells channel selected ($allCellsChannel) does not exist. There are ${imageChannels.size} channels available")
        }

        return analyseTransduction(
            imageChannels[targetChannel - 1],
            imageChannels[transducedChannel - 1],
            if (isAllCellsEnabled()) imageChannels[allCellsChannel - 1] else null
        )
    }

    fun analyseTransduction(targetChannel: ImagePlus, transducedChannel: ImagePlus, allCellsChannel: ImagePlus?): TransductionResult {
        logService.info("Starting extraction")
        // TODO(#77)
        val preprocessingParameters = PreprocessingParameters(largestCellDiameter)
        val targetCells = cellSegmentationService.extractCells(targetChannel, preprocessingParameters)
        val transducedCells = filterCellsByIntensity(cellSegmentationService.extractCells(transducedChannel, preprocessingParameters), transducedChannel)
        val allCells = if (allCellsChannel != null) cellSegmentationService.extractCells(allCellsChannel, preprocessingParameters) else null

        logService.info("Starting analysis")

        val targetTransducedAnalysis = BucketedNaiveColocalizer(
            largestCellDiameter.toInt(),
            targetChannel.width,
            targetChannel.height,
            PixelCellComparator(threshold = 0.01f)
        ).analyseColocalization(targetCells, transducedCells)

        var overlappingAllCells: List<PositionedCell>? = null
        if (allCells != null) {
            overlappingAllCells = BucketedNaiveColocalizer(
                largestCellDiameter.toInt(),
                allCellsChannel!!.width,
                allCellsChannel.height,
                PixelCellComparator(threshold = 0.01f)
            ).analyseColocalization(targetTransducedAnalysis.overlapping, allCells).overlapping
        }

        return TransductionResult(
            targetCellCount = targetCells.size,
            overlappingTransducedCellAnalyses = cellColocalizationService.analyseCellIntensity(
                transducedChannel,
                targetTransducedAnalysis.overlapping.map { it.toRoi() }.toTypedArray()
            ),
            overlappingTwoChannelCells = targetTransducedAnalysis.overlapping,
            overlappingThreeChannelCells = overlappingAllCells
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
            ij.command().run(SimpleColocalization::class.java, true)
        }
    }
}

class ChannelDoesNotExistException(message: String) : Exception(message)
