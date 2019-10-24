package simplecolocalization

import ij.ImagePlus
import ij.gui.Roi
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.ChannelSplitter
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.filter.RankFilters
import ij.plugin.frame.RoiManager
import ij.process.ImageConverter
import java.io.File
import net.imagej.ImageJ
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import org.scijava.widget.NumberWidget

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
@Plugin(type = Command::class, menuPath = "Plugins > Simple Colocalization")
class SimpleColocalization : Command {

    /**
     * Entry point for UI operations, automatically handling both graphical and
     * headless use of this plugin.
     */
    @Parameter
    private lateinit var uiService: UIService

    /** File path of the input image. */
    @Parameter(label = "Input Image")
    private lateinit var imageFile: File

    @Parameter(
        label = "Preprocessing Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var preprocessingParamsHeader: String

    /**
     * Applied to the input image to reduce sensitivity of the thresholding
     * algorithm. Higher value means more blur.
     */
    @Parameter(
        label = "Gaussian Blur Sigma (Radius)",
        description = "Reduces sensitivity to cell edges by blurring the " +
            "overall image. Higher is less sensitive.",
        min = "0.0",
        stepSize = "1.0",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var gaussianBlurSigma = 3.0

    private var meanGreenThreshold = 30.0

    @Parameter(
        label = "Cell Identification Parameters:",
        visibility = ItemVisibility.MESSAGE,
        required = false
    )
    private lateinit var identificationParamsHeader: String

    /**
     * Used during the cell identification stage to reduce overlapping cells
     * being grouped into a single cell.
     *
     * TODO (#5): Figure out what this value should be.
     */
    @Parameter(
        label = "Largest Cell Diameter",
        min = "5.0",
        stepSize = "1.0",
        style = NumberWidget.SPINNER_STYLE,
        required = true,
        persist = false
    )
    private var largestCellDiameter = 30.0

    /**
     * Displays the resulting counts as a results table.
     */
    private fun showCount(analyses: Array<CellAnalysis>) {
        val table = DefaultGenericTable()
        val cellCountColumn = IntColumn()
        val greenCountColumn = IntColumn()
        cellCountColumn.add(analyses.size)
        var greenCount = 0
        analyses.forEach { cellAnalysis ->
            if (cellAnalysis.channels[1].mean > meanGreenThreshold) {
                greenCount++
            }
        }
        greenCountColumn.add(greenCount)
        table.add(cellCountColumn)
        table.add(greenCountColumn)
        table.setColumnHeader(0, "Red Cell Count")
        table.setColumnHeader(1, "Green Cell Count")
        uiService.show(table)
    }

    /**
     * Perform pre-processing on the image to remove background and set cells to white.
     */
    private fun preprocessImage(image: ImagePlus) {
        // Convert to grayscale 8-bit.
        ImageConverter(image).convertToGray8()

        // Remove background.
        BackgroundSubtracter().rollingBallBackground(
            image.channelProcessor,
            largestCellDiameter,
            false,
            false,
            false,
            false,
            false
        )

        // Threshold grayscale image, leaving black and white image.
        image.channelProcessor.autoThreshold()

        // Despeckle the image using a median filter with radius 1.0, as defined in ImageJ docs.
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/RankFilters.html
        RankFilters().rank(image.channelProcessor, 1.0, RankFilters.MEDIAN)

        // Apply Gaussian Blur to group larger speckles.
        image.channelProcessor.blurGaussian(gaussianBlurSigma)

        // Threshold image to remove blur.
        image.channelProcessor.autoThreshold()
    }

    /**
     * Segment the image into individual cells, overlaying outlines for cells in the image.
     *
     * Uses ImageJ's Euclidean Distance Map plugin for performing the watershed algorithm.
     * Used as a simple starting point that'd allow for cell counting.
     */
    private fun segmentImage(image: ImagePlus) {
        // TODO (#7): Review and improve upon simple watershed.
        EDM().toWatershed(image.channelProcessor)
    }

    /** Runs after the parameters above are populated. */
    override fun run() {
        val originalImage = ImagePlus(imageFile.absolutePath)
        val image = ImagePlus(imageFile.absolutePath)

        preprocessImage(image)
        segmentImage(image)

        val roiManager = RoiManager.getRoiManager()
        val cells = identifyCells(roiManager, image)
        markCells(originalImage, cells)

        val analysis = analyseCells(originalImage, cells)

        showCount(analysis)
        showPerCellAnalysis(analysis)

        originalImage.show()
    }

    /**
     * Select each cell identified in the segmented image in the original image.
     *
     * We use [ParticleAnalyzer] instead of [MaximumFinder] as the former highlights the shape of the cell instead
     * of just marking its centre.
     */
    private fun identifyCells(roiManager: RoiManager, segmentedImage: ImagePlus): Array<Roi> {
        ParticleAnalyzer.setRoiManager(roiManager)
        ParticleAnalyzer(
            ParticleAnalyzer.SHOW_NONE or ParticleAnalyzer.ADD_TO_MANAGER,
            Measurements.ALL_STATS,
            ResultsTable(),
            0.0,
            Double.MAX_VALUE
        ).analyze(segmentedImage)
        return roiManager.roisAsArray
    }

    /**
     * Mark the cell locations in the image.
     */
    private fun markCells(image: ImagePlus, rois: Array<Roi>) {
        for (roi in rois) {
            roi.image = image
        }
    }

    data class CellAnalysis(val area: Int, val channels: List<ChannelAnalysis>)
    data class ChannelAnalysis(val name: String, val mean: Int, val min: Int, val max: Int)

    /**
     * Analyses the channel intensity of the cells.
     */
    private fun analyseCells(image: ImagePlus, highlightedCells: Array<Roi>): Array<CellAnalysis> {
        // Split the image into multiple grayscale images (one for each channel)
        val channelImages = ChannelSplitter.split(image)
        val numberOfChannels = channelImages.size

        val analyses = arrayListOf<CellAnalysis>()
        for (cell in highlightedCells) {
            var area = 0
            val sums = MutableList(numberOfChannels) { 0 }
            val mins = MutableList(numberOfChannels) { Integer.MAX_VALUE }
            val maxs = MutableList(numberOfChannels) { Integer.MIN_VALUE }
            val containedCells = cell.containedPoints
            containedCells.forEach { point ->
                area++
                for (channel in 0 until numberOfChannels) {
                    // pixelData is of the form [value, 0, 0, 0] because ImageJ
                    val pixelData = channelImages[channel].getPixel(point.x, point.y)
                    sums[channel] += pixelData[0]
                    mins[channel] = Integer.min(mins[channel], pixelData[0])
                    maxs[channel] = Integer.max(maxs[channel], pixelData[0])
                }
            }
            val channels = mutableListOf<ChannelAnalysis>()
            for (channel in 0 until numberOfChannels) {
                channels.add(
                    ChannelAnalysis(
                        channelImages[channel].title,
                        sums[channel] / area,
                        mins[channel],
                        maxs[channel]
                    )
                )
            }
            analyses.add(CellAnalysis(area, channels))
        }

        return analyses.toTypedArray()
    }

    /**
     * Displays the resulting cell analysis as a results table.
     */
    private fun showPerCellAnalysis(analyses: Array<CellAnalysis>) {
        val table = DefaultGenericTable()

        // If there are no analyses then show an empty table
        // We wish to access the first analysis later to inspect number of channels
        // so we return to avoid an invalid deference
        if (analyses.isEmpty()) {
            uiService.show(table)
            return
        }
        val numberOfChannels = analyses[0].channels.size

        // Retrieve the names of all the channels
        val channelNames = mutableListOf<String>()
        for (i in 0 until numberOfChannels) {
            channelNames.add(analyses[0].channels[i].name.capitalize())
        }

        val areaColumn = IntColumn()

        val meanColumns = MutableList(numberOfChannels) { IntColumn() }
        val maxColumns = MutableList(numberOfChannels) { IntColumn() }
        val minColumns = MutableList(numberOfChannels) { IntColumn() }

        // Construct column values using the channel analysis values
        analyses.forEach { cellAnalysis ->
            areaColumn.add(cellAnalysis.area)
            cellAnalysis.channels.forEachIndexed { channelIndex, channel ->
                meanColumns[channelIndex].add(channel.mean)
                minColumns[channelIndex].add(channel.min)
                maxColumns[channelIndex].add(channel.max)
            }
        }

        // Add all of the columns (Mean, Min, Max) for each channel
        table.add(areaColumn)
        for (i in 0 until numberOfChannels) {
            table.add(meanColumns[i])
            table.add(minColumns[i])
            table.add(maxColumns[i])
        }

        // Add all the column headers for each channel
        var columnIndex = 0
        table.setColumnHeader(columnIndex++, "Area")
        for (i in 0 until numberOfChannels) {
            val channelName = channelNames[i]
            table.setColumnHeader(columnIndex++, "$channelName Mean")
            table.setColumnHeader(columnIndex++, "$channelName Min")
            table.setColumnHeader(columnIndex++, "$channelName Max")
        }

        uiService.show(table)
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
            ij.ui().showUI()
            ij.command().run(SimpleColocalization::class.java, true)
        }
    }
}
