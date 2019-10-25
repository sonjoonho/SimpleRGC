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
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service

@Plugin(type = Service::class)
class CellSegmentationService : AbstractService(), ImageJService {

    /**
     * Perform pre-processing on the image to remove background and set cells to white.
     */
    fun preprocessImage(image: ImagePlus, largestCellDiameter: Double, gaussianBlurSigma: Double) {
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
    fun segmentImage(image: ImagePlus) {
        // TODO(#7): Review and improve upon simple watershed.
        EDM().toWatershed(image.channelProcessor)
    }

    /**
     * Select each cell identified in the segmented image in the original image.
     *
     * We use [ParticleAnalyzer] instead of [MaximumFinder] as the former highlights the shape of the cell instead
     * of just marking its centre.
     */
    fun identifyCells(roiManager: RoiManager, segmentedImage: ImagePlus): Array<Roi> {
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
    fun markCells(image: ImagePlus, rois: Array<Roi>) {
        for (roi in rois) {
            roi.image = image
        }
    }

    /**
     * Analyses the channel intensity of the cells.
     */
    fun analyseCells(image: ImagePlus, highlightedCells: Array<Roi>): Array<SimpleColocalization.CellAnalysis> {
        // Split the image into multiple grayscale images (one for each channel).
        val channelImages = ChannelSplitter.split(image)
        val numberOfChannels = channelImages.size

        val analyses = arrayListOf<SimpleColocalization.CellAnalysis>()
        for (cell in highlightedCells) {
            var area = 0
            val sums = MutableList(numberOfChannels) { 0 }
            val mins = MutableList(numberOfChannels) { Integer.MAX_VALUE }
            val maxs = MutableList(numberOfChannels) { Integer.MIN_VALUE }
            val containedCells = cell.containedPoints
            containedCells.forEach { point ->
                area++
                for (channel in 0 until numberOfChannels) {
                    // pixelData is of the form [value, 0, 0, 0] because ImageJ.
                    val pixelData = channelImages[channel].getPixel(point.x, point.y)
                    sums[channel] += pixelData[0]
                    mins[channel] = Integer.min(mins[channel], pixelData[0])
                    maxs[channel] = Integer.max(maxs[channel], pixelData[0])
                }
            }
            val channels = mutableListOf<SimpleColocalization.ChannelAnalysis>()
            for (channel in 0 until numberOfChannels) {
                channels.add(
                    SimpleColocalization.ChannelAnalysis(
                        channelImages[channel].title,
                        sums[channel] / area,
                        mins[channel],
                        maxs[channel]
                    )
                )
            }
            analyses.add(SimpleColocalization.CellAnalysis(area, channels))
        }

        return analyses.toTypedArray()
    }
}
