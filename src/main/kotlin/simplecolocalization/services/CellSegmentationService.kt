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
import ij.process.AutoThresholder
import ij.process.ImageConverter
import io.minio.errors.InvalidArgumentException
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service
import simplecolocalization.GlobalThresholdAlgos
import simplecolocalization.LocalThresholdAlgos
import simplecolocalization.PreprocessingParameters
import simplecolocalization.ThresholdTypes
import simplecolocalization.bernsen
import simplecolocalization.niblack
import simplecolocalization.otsu

@Plugin(type = Service::class)
class CellSegmentationService : AbstractService(), ImageJService {

    data class CellAnalysis(val area: Int, val channels: List<ChannelAnalysis>)
    data class ChannelAnalysis(val name: String, val mean: Int, val min: Int, val max: Int)

    /** Perform pre-processing on the image to remove background and set cells to white. */
    fun preprocessImage(
        image: ImagePlus,
        params: PreprocessingParameters
    ) {
        // Convert to grayscale 8-bit.
        ImageConverter(image).convertToGray8()

        if (params.shouldSubtractBackground) {
            // Remove background.
            BackgroundSubtracter().rollingBallBackground(
                image.channelProcessor,
                params.largestCellDiameter,
                false,
                false,
                false,
                false,
                false
            )
        }

        thresholdImage(image, params.thresholdLocality, params.globalThresholdAlgo, params.localThresholdAlgo, params.localThresholdRadius)

        // Despeckle the image using a median filter with radius 1.0, as defined in ImageJ docs.
        // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/RankFilters.html
        RankFilters().rank(image.channelProcessor, 1.0, RankFilters.MEDIAN)
        if (params.shouldDespeckle) {
            // Despeckle the image using a median filter with radius 1.0, as defined in ImageJ docs.
            // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/RankFilters.html
            RankFilters().rank(image.channelProcessor, params.despeckleRadius, RankFilters.MEDIAN)
        }

        if (params.shouldGaussianBlur) {
            // Apply Gaussian Blur to group larger speckles.
            image.channelProcessor.blurGaussian(params.gaussianBlurSigma)
        }

        // Threshold image to remove blur.
        image.channelProcessor.autoThreshold()
        // Threshold image again to remove blur.
        thresholdImage(image, ThresholdTypes.GLOBAL, params.globalThresholdAlgo, params.localThresholdAlgo, params.localThresholdRadius)
    }

    private fun thresholdImage(image: ImagePlus, thresholdChoice: String, globalThresholdAlgo: String, localThresholdAlgo: String, localThresholdRadius: Int) {
        when (thresholdChoice) {
            ThresholdTypes.GLOBAL -> {
                when (globalThresholdAlgo) {
                    GlobalThresholdAlgos.OTSU -> image.processor.setAutoThreshold(
                        AutoThresholder.Method.Otsu,
                        true
                    )
                    GlobalThresholdAlgos.MOMENTS -> image.processor.setAutoThreshold(
                        AutoThresholder.Method.Moments,
                        true
                    )
                    GlobalThresholdAlgos.SHANBHAG -> image.processor.setAutoThreshold(
                        AutoThresholder.Method.Shanbhag,
                        true
                    )
                    else -> throw InvalidArgumentException("Threshold Algorithm selected")
                }
                image.processor.autoThreshold()
            }
            ThresholdTypes.LOCAL -> {
                when (localThresholdAlgo) {
                    LocalThresholdAlgos.OTSU -> otsu(
                        image,
                        localThresholdRadius
                    )
                    LocalThresholdAlgos.BERNSEN -> bernsen(
                        image,
                        localThresholdRadius,
                        15.0
                    ) // TODO(rasnav99): Decide additional parameters for these methods.
                    LocalThresholdAlgos.NIBLACK -> niblack(
                        image,
                        localThresholdRadius,
                        0.2,
                        0.0
                    )
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
