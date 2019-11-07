package simplecolocalization.services

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
import io.minio.errors.InvalidArgumentException
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service
import simplecolocalization.commands.SimpleCellCounter
import simplecolocalization.utils.bernsen
import simplecolocalization.utils.niblack
import simplecolocalization.utils.otsu

@Plugin(type = Service::class)
class CellSegmentationService : AbstractService(), ImageJService {

    data class CellAnalysis(val area: Int, val channels: List<ChannelAnalysis>)
    data class ChannelAnalysis(val name: String, val mean: Int, val min: Int, val max: Int)

    /** Preprocess image wrapper for SimpleColocalisation Plugin (temp fix). */
    fun preprocessImage(
        image: ImagePlus,
        largestCellDiameter: Double,
        gaussianBlurSigma: Double
    ) {
        preprocessImage(
            image, largestCellDiameter,
            gaussianBlurSigma
        )
    }

    /** Perform pre-processing on the image to remove background and set cells to white. */
    fun preprocessImage(
        image: ImagePlus,
        largestCellDiameter: Double,
        gaussianBlurSigma: Double,
        shouldSubtractBackground: Boolean = true,
        thresholdLocality: String = SimpleCellCounter.ThresholdTypes.GLOBAL,
        globalThresholdAlgo: String = SimpleCellCounter.GlobalThresholdAlgos.OTSU,
        localThresholdAlgo: String = SimpleCellCounter.LocalThresholdAlgos.OTSU,
        localThresholdRadius: Int = 15,
        shouldDespeckle: Boolean = true,
        despeckleRadius: Double = 1.0,
        shouldGaussianBlur: Boolean = true
    ) {
        // Convert to grayscale 8-bit.
        ImageConverter(image).convertToGray8()

        if (shouldSubtractBackground) {
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
        }

        // Threshold grayscale image, leaving black and white image.
        thresholdImage(image, thresholdLocality, globalThresholdAlgo, localThresholdAlgo, localThresholdRadius)

        if (shouldDespeckle) {
            // Despeckle the image using a median filter with radius 1.0, as defined in ImageJ docs.
            // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/RankFilters.html
            RankFilters().rank(image.channelProcessor, despeckleRadius, RankFilters.MEDIAN)
        }

        if (shouldGaussianBlur) {
            // Apply Gaussian Blur to group larger speckles.
            image.channelProcessor.blurGaussian(gaussianBlurSigma)
        }

        // Threshold image again to remove blur.
        thresholdImage(image, SimpleCellCounter.ThresholdTypes.GLOBAL, globalThresholdAlgo, localThresholdAlgo, localThresholdRadius)
    }

    fun thresholdImage(image: ImagePlus, thresholdChoice: String, globalThresholdAlgo: String, localThresholdAlgo: String, localThresholdRadius: Int) {
        when (thresholdChoice) {
            SimpleCellCounter.ThresholdTypes.GLOBAL -> {
                image.channelProcessor.setAutoThreshold(globalThresholdAlgo)
                image.channelProcessor.autoThreshold()
            }
            SimpleCellCounter.ThresholdTypes.LOCAL -> {
                when (localThresholdAlgo) {
                    SimpleCellCounter.LocalThresholdAlgos.OTSU -> otsu(image, localThresholdRadius)
                    SimpleCellCounter.LocalThresholdAlgos.BERNSEN -> bernsen(image, localThresholdRadius, 15.0) // Not sure what additional param ought to be
                    SimpleCellCounter.LocalThresholdAlgos.NIBLACK -> niblack(image, localThresholdRadius, 0.2, 0.0) // Not sure what additional params ought to be
                    else -> throw InvalidArgumentException("Threshold Algorithm selected")
                }
            }
            else -> throw InvalidArgumentException("Invalid Threshold Choice selected")
        }
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

    /** Mark the cell locations in the image. */
    fun markCells(image: ImagePlus, rois: Array<Roi>) {
        for (roi in rois) {
            roi.image = image
        }
    }
}
