package simplecolocalization

import ij.ImagePlus
import ij.gui.Roi
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.filter.RankFilters
import ij.plugin.frame.RoiManager
import ij.process.ImageConverter
import net.imagej.ImageJ
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.table.DefaultGenericTable
import org.scijava.table.IntColumn
import org.scijava.ui.UIService
import org.scijava.widget.NumberWidget
import java.io.File

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

    private val numRgbChannels = 3

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

    /** Displays the resulting count as a results table. */
    private fun showCount(count: Int) {
        val table = DefaultGenericTable()
        val countColumn = IntColumn()
        countColumn.add(count)
        table.add(countColumn)
        table.setColumnHeader(0, "Count")
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

        showCount(cells.size)
        showCellAnalysis(analysis)
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
    data class ChannelAnalysis(val mean: Int, val min: Int, val max: Int)

    /**
     * Analyses the RGB intensity of the cells.
     */
    private fun analyseCells(image: ImagePlus, highlightedCells: Array<Roi>): Array<CellAnalysis> {
        // Convert to RGB so that we can get the r,g,b for each pixel
        ImageConverter(image).convertToRGB()

        // Image will have 3 channels as it is RGB following conversion
        val analyses = arrayListOf<CellAnalysis>()
        for (cell in highlightedCells) {
            var area = 0
            val sums = MutableList(numRgbChannels) { 0 }
            val mins = MutableList(numRgbChannels) { Integer.MAX_VALUE }
            val maxs = MutableList(numRgbChannels) { Integer.MIN_VALUE }
            val containedCells = cell.containedPoints
            containedCells.forEach { point ->
                // pixelData is of the form [r, g, b, 0]
                val pixelData = image.getPixel(point.x, point.y)
                area++
                for (channel in 0 until numRgbChannels) {
                    sums[channel] += pixelData[channel]
                    mins[channel] = Integer.min(mins[channel], pixelData[channel])
                    maxs[channel] = Integer.max(maxs[channel], pixelData[channel])
                }
            }
            val channels = mutableListOf<ChannelAnalysis>()
            for (channel in 0 until numRgbChannels) {
                channels.add(ChannelAnalysis(sums[channel] / area, mins[channel], maxs[channel]))
            }
            analyses.add(CellAnalysis(area, channels))
        }

        return analyses.toTypedArray()
    }

    /** Displays the resulting cell analysis as a results table. */
    private fun showCellAnalysis(analyses: Array<CellAnalysis>) {
        val table = DefaultGenericTable()

        val areaColumn = IntColumn()
        val redMeanColumn = IntColumn()
        val redMinColumn = IntColumn()
        val redMaxColumn = IntColumn()
        val greenMeanColumn = IntColumn()
        val greenMinColumn = IntColumn()
        val greenMaxColumn = IntColumn()
        val blueMeanColumn = IntColumn()
        val blueMinColumn = IntColumn()
        val blueMaxColumn = IntColumn()

        analyses.forEachIndexed { index, channelAnalysis ->
            areaColumn.add(channelAnalysis.area)

            redMeanColumn.add(channelAnalysis.channels[0].mean)
            redMinColumn.add(channelAnalysis.channels[0].min)
            redMaxColumn.add(channelAnalysis.channels[0].max)
            greenMeanColumn.add(channelAnalysis.channels[1].mean)
            greenMinColumn.add(channelAnalysis.channels[1].min)
            greenMaxColumn.add(channelAnalysis.channels[1].max)
            blueMeanColumn.add(channelAnalysis.channels[2].mean)
            blueMinColumn.add(channelAnalysis.channels[2].min)
            blueMaxColumn.add(channelAnalysis.channels[2].max)
        }

        table.add(areaColumn)
        table.add(redMeanColumn)
        table.add(redMinColumn)
        table.add(redMaxColumn)
        table.add(greenMeanColumn)
        table.add(greenMinColumn)
        table.add(greenMaxColumn)
        table.add(blueMeanColumn)
        table.add(blueMinColumn)
        table.add(blueMaxColumn)

        table.setColumnHeader(0, "Area")
        table.setColumnHeader(1, "Red Mean")
        table.setColumnHeader(2, "Red Min")
        table.setColumnHeader(3, "Red Max")
        table.setColumnHeader(4, "Green Mean")
        table.setColumnHeader(5, "Green Min")
        table.setColumnHeader(6, "Green Max")
        table.setColumnHeader(7, "Blue Mean")
        table.setColumnHeader(8, "Blue Min")
        table.setColumnHeader(9, "Blue Max")

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
