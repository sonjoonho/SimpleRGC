package simplecolocalization.services

import de.biomedical_imaging.ij.steger.LineDetector
import fiji.threshold.Auto_Local_Threshold
import ij.ImagePlus
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.ZProjector
import ij.plugin.filter.EDM
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.process.AutoThresholder
import ij.process.FloatPolygon
import ij.process.ImageConverter
import java.awt.Color
import java.lang.Math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import net.imagej.ImageJService
import org.scijava.app.StatusService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service
import simplecolocalization.DummyRoiManager
import simplecolocalization.services.colocalizer.PositionedCell

// Constants used in estimating parameters for axon removal
// Taken from https://github.com/thorstenwagner/ij-ridgedetection
const val CONTRAST_LOW = 87
const val CONTRAST_HIGH = 230
const val THRESHOLD_MULTIPLIER = 0.17

// Optimal parameter for axon removal based on experimentation
const val AXON_WIDTH = 3.5

@Plugin(type = Service::class)
class CellSegmentationService : AbstractService(), ImageJService {

    @Parameter
    private lateinit var statusService: StatusService

    /** Perform pre-processing on the image to remove background and set cells to white. */
    private fun preprocessImage(
        image: ImagePlus,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        shouldRemoveAxons: Boolean
    ) {
        // Convert to grayscale 8-bit.
        ImageConverter(image).convertToGray8()

        // Additional params with values 0.0 are unused. Just required by localthreshold api.
        Auto_Local_Threshold().exec(
            image,
            "Niblack",
            localThresholdRadius,
            0.0,
            0.0,
            true
        )

        if (shouldRemoveAxons) {
            removeAxons(image, detectAxons(image, AXON_WIDTH))
        }

        // Apply Gaussian Blur to group larger speckles.
        image.channelProcessor.blurGaussian(gaussianBlurSigma)

        // Threshold image again to remove blur.
        image.processor.setAutoThreshold(AutoThresholder.Method.Otsu, true)
        image.processor.autoThreshold()
    }

    /**
     * Extract a list of cells from the specified image.
     */
    fun extractCells(
        image: ImagePlus,
        diameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        shouldRemoveAxons: Boolean = false
    ): List<PositionedCell> {

        val mutableImage = if (image.nSlices > 1) {
            // Flatten slices of the image. This step should probably be done during inside the pre-processing step -
            // however this operation is not done in-place but creates a new image, which makes this hard.
            ZProjector.run(image, "max")
        } else {
            image.duplicate()
        }

        statusService.showStatus(25, 100, "Preprocessing...")
        preprocessImage(mutableImage, localThresholdRadius, gaussianBlurSigma, shouldRemoveAxons)
        statusService.showStatus(50, 100, "Segmenting image...")
        segmentImage(mutableImage)
        statusService.showStatus(75, 100, "Identifying cells...")

        return identifyCells(
            mutableImage,
            diameterRange.smallest,
            diameterRange.largest
        )
    }

    /**
     * Detect axons/dendrites within an image and return them as Rois.
     *
     * Uses Ridge Detection plugin's LineDetector.
     */
    private fun detectAxons(image: ImagePlus, axonWidth: Double): List<Roi> {

        // Estimate parameters for removing axons
        val sigma = estimateSigma(axonWidth)
        val lowerThresh = estimateThreshold(axonWidth, sigma, CONTRAST_LOW)
        val upperThresh = estimateThreshold(axonWidth, sigma, CONTRAST_HIGH)

        val contours = LineDetector().detectLines(
            image.processor, sigma, upperThresh, lowerThresh,
            0.0, Double.MAX_VALUE, false, true, true, true
        )
        val axons = mutableListOf<Roi>()
        // Convert to Rois.
        for (c in contours) {
            // Generate one Roi per contour.
            val p = FloatPolygon(c.xCoordinates, c.yCoordinates, c.number)
            val r = PolygonRoi(p, Roi.FREELINE)
            r.position = c.frame
            // Set the line width of Roi based on the average width of axon.
            var sumWidths = 0.0
            for (j in c.lineWidthL.indices) {
                sumWidths += c.lineWidthL[j] + c.lineWidthR[j]
            }
            r.strokeWidth = (sumWidths / (c.xCoordinates.size)).toFloat()
            axons.add(r)
        }
        return axons
    }

    private fun estimateSigma(lineWidth: Double) = lineWidth / (2 * sqrt(3.0)) + 0.5

    private fun estimateThreshold(lineWidth: Double, sigma: Double, contrast: Int): Double {
        return floor(
            abs(
                -2 * contrast * (lineWidth / 2.0) /
                    (sqrt(2 * PI) * sigma.pow(3)) *
                    exp(-((lineWidth / 2.0) * (lineWidth / 2.0)) / (2 * sigma * sigma))
            )
        ) * THRESHOLD_MULTIPLIER
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
     * Appropriate pre-processing is expected before calling this.
     */
    private fun segmentImage(image: ImagePlus) {
        // Preprocessing is good enough that watershed is sufficient to segment here.
        EDM().toWatershed(image.channelProcessor)
    }

    /**
     * Select each cell identified in the segmented image in the original image.
     *
     * We use [ParticleAnalyzer] instead of [MaximumFinder] as the former highlights the shape of the cell instead
     * of just marking its centre.
     */
    fun identifyCells(
        segmentedImage: ImagePlus,
        smallestCellDiameter: Double,
        largestCellDiameter: Double
    ): List<PositionedCell> {
        // Compute min/max area
        val minArea = smallestCellDiameter.diameterToArea()
        val maxArea = largestCellDiameter.diameterToArea()

        val roiManager = DummyRoiManager()
        ParticleAnalyzer.setRoiManager(roiManager)
        ParticleAnalyzer(
            ParticleAnalyzer.SHOW_NONE or ParticleAnalyzer.ADD_TO_MANAGER,
            Measurements.ALL_STATS,
            ResultsTable(),
            minArea,
            maxArea
        ).analyze(segmentedImage)
        return roiManager.roisAsArray.map { PositionedCell.fromRoi(it) }
    }
}

/** Compute the area from the diameter. */
fun Double.diameterToArea(): Double = (this / 2).pow(2.0) * PI
