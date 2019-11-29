package simplecolocalization.services

import de.biomedical_imaging.ij.steger.LineDetector
import ij.ImagePlus
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.filter.RankFilters
import ij.process.AutoThresholder
import ij.process.FloatPolygon
import ij.process.ImageConverter
import java.awt.Color
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service
import simplecolocalization.DummyRoiManager
import simplecolocalization.algorithms.bernsen
import simplecolocalization.algorithms.niblack
import simplecolocalization.algorithms.otsu
import simplecolocalization.preprocessing.LocalThresholdAlgos
import simplecolocalization.preprocessing.PreprocessingParameters
import simplecolocalization.preprocessing.ThresholdTypes
import simplecolocalization.services.colocalizer.PositionedCell

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
                params.largestCellDiameter.toDouble(),
                false,
                false,
                false,
                false,
                false
            )
        }

        when (params.thresholdLocality) {
            ThresholdTypes.GLOBAL -> {
                image.processor.setAutoThreshold(AutoThresholder.Method.Otsu, true)
                image.processor.autoThreshold()
            }
            ThresholdTypes.LOCAL -> {
                when (params.localThresholdAlgo) {
                    LocalThresholdAlgos.OTSU -> otsu(
                        image,
                        params.largestCellDiameter.toInt()
                    )
                    LocalThresholdAlgos.BERNSEN -> bernsen(
                        image,
                        params.largestCellDiameter.toInt(),
                        15.0
                    )
                    LocalThresholdAlgos.NIBLACK -> niblack(
                        image,
                        params.largestCellDiameter.toInt(),
                        0.2,
                        0.0
                    )
                    else -> throw IllegalArgumentException("Threshold Algorithm selected")
                }
            }
            else -> throw IllegalArgumentException("Invalid Threshold Choice selected")
        }

        if (params.shouldDespeckle) {
            // Despeckle the image using a median filter with radius 1.0, as defined in ImageJ docs.
            // https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/RankFilters.html
            RankFilters().rank(image.channelProcessor, params.despeckleRadius, RankFilters.MEDIAN)
        }

        removeAxons(image, detectAxons(image))

        if (params.shouldGaussianBlur) {
            // Apply Gaussian Blur to group larger speckles.
            image.channelProcessor.blurGaussian(params.gaussianBlurSigma)
        }

        // Threshold image again to remove blur.
        image.processor.setAutoThreshold(AutoThresholder.Method.Otsu, true)
        image.processor.autoThreshold()
    }

    /**
     * Detect axons/dendrites within an image and return them as Rois.
     *
     * Uses Ridge Detection plugin's LineDetector.
     */
    private fun detectAxons(image: ImagePlus): List<Roi> {
        // Empirically, the values of sigma, upperThresh and lowerThresh
        // proved the most effective on test images
        val contours = LineDetector().detectLines(
            image.processor, 1.61, 15.0, 5.0,
            0.0, 0.0, false, true, true, true
        )
        val axons = mutableListOf<Roi>()
        // Convert to Rois
        for (c in contours) {
            val p = FloatPolygon(c.xCoordinates, c.yCoordinates, c.number)
            val r = PolygonRoi(p, Roi.FREELINE)
            r.position = c.frame
            // Set the line width of Roi based on the average width of axon
            var sumWidths = 0.0
            for (j in c.lineWidthL.indices) {
                sumWidths += c.lineWidthL[j] + c.lineWidthR[j]
            }
            r.strokeWidth = (sumWidths / (c.xCoordinates.size)).toFloat()
            axons.add(r)
        }
        return axons
    }

    /** Remove axon rois from the (thresholded) image. */
    private fun removeAxons(image: ImagePlus, axons: List<Roi>) {
        // The background post-thresholding will be black
        // therefore we want the brush to be black
        image.setColor(Color.BLACK)
        for (axon in axons) {
            axon.drawPixels(image.processor)
        }
    }

    /**
     * Segment the image into individual cells, overlaying outlines for cells in the image.
     *
     * Uses ImageJ's Euclidean Distance Map plugin for performing the watershed algorithm.
     * Used as a simple starting point that'd allow for cell counting.
     */
    fun segmentImage(image: ImagePlus) {
        // Preprocessing is good enough that watershed is sufficient to segment here.
        EDM().toWatershed(image.channelProcessor)
    }

    /**
     * Select each cell identified in the segmented image in the original image.
     *
     * We use [ParticleAnalyzer] instead of [MaximumFinder] as the former highlights the shape of the cell instead
     * of just marking its centre.
     */
    fun identifyCells(segmentedImage: ImagePlus): List<PositionedCell> {
        val roiManager = DummyRoiManager()
        ParticleAnalyzer.setRoiManager(roiManager)
        ParticleAnalyzer(
            ParticleAnalyzer.SHOW_NONE or ParticleAnalyzer.ADD_TO_MANAGER,
            Measurements.ALL_STATS,
            ResultsTable(),
            0.0,
            Double.MAX_VALUE
        ).analyze(segmentedImage)
        return roiManager.roisAsArray.map { PositionedCell.fromRoi(it) }
    }
}
