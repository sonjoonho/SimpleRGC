package simplecolocalization.services

import ij.IJ
import ij.ImagePlus
import ij.gui.NewImage.FILL_BLACK
import ij.gui.NewImage.createByteImage
import ij.gui.OvalRoi
import ij.gui.Roi
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.filter.RankFilters
import ij.plugin.frame.RoiManager
import ij.process.Blitter
import ij.process.ImageConverter
import ij.process.ImageProcessor
import io.minio.errors.InvalidArgumentException
import kotlin.experimental.and
import kotlin.math.sqrt
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service

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
        preprocessImage(image, true, largestCellDiameter, "Global", "Otsu's", 30, true, 1.0, true, gaussianBlurSigma)
    }

    /** Perform pre-processing on the image to remove background and set cells to white. */
    fun preprocessImage(
        image: ImagePlus,
        shouldSubtractBackground: Boolean,
        largestCellDiameter: Double,
        thresholdLocality: String,
        thresholdAlgo: String,
        localThresholdRadius: Int,
        shouldDespeckle: Boolean,
        despeckleRadius: Double,
        shouldGaussianBlur: Boolean,
        gaussianBlurSigma: Double
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
        thresholdImage(image, thresholdLocality, thresholdAlgo, localThresholdRadius)

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
        thresholdImage(image, thresholdLocality, thresholdAlgo, localThresholdRadius)
    }

    /** Copied with modification from FIJI's auto local thresholding algorithms: https://github.com/fiji/Auto_Local_Threshold */
    private fun duplicateImage(iProcessor: ImageProcessor): ImagePlus {
        val w = iProcessor.width
        val h = iProcessor.height
        val iPlus = createByteImage("Image", w, h, 1, FILL_BLACK)
        val imageProcessor = iPlus.processor
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY)
        return iPlus
    }

    fun otsu(image: ImagePlus, radius: Int) {
        ImageConverter(image).convertToGray8()
        val width = image.width
        val height = image.height
        var position: Int
        val imageProcessor = image.processor
        val pixels = imageProcessor.pixels as ByteArray
        val thresholdedPixels = ByteArray(pixels.size)
        val l = 256 // 256 is for 8bit images
        var totalMean: Double // mean gray-level for the whole image
        var interClassVariance: Double
        var scale: Double // scaling term for inter-class variance
        var maxInterClassVariance: Double
        val histogram = DoubleArray(l) // normalized histogram
        val cumulativeHistogram = DoubleArray(l) // cumulative normalized histogram
        val mean = DoubleArray(l) // mean gray-level

        val roi = OvalRoi(0, 0, 2*radius, 2*radius)
        for (y in 0 until height) {
            // Show progress bar.
            IJ.showProgress(y.toDouble() / (height - 1))

            for (x in 0 until width) {
                roi.setLocation(x - radius, y - radius)
                imageProcessor.setRoi(roi)
                position = x + y * width
                val data = imageProcessor.histogram

                // Count number of pixels
                var numPixels = 0
                for (j in 0 until l) numPixels += data[j]

                scale = 1 / numPixels.toDouble()

                histogram[0] = scale * data[0] // normalized histogram
                cumulativeHistogram[0] = histogram[0] // cumulative normalized histogram
                mean[0] = 0.0 // mean grey-level
                for (j in 1 until l) {
                    histogram[j] = scale * data[j]
                    cumulativeHistogram[j] = cumulativeHistogram[j - 1] + histogram[j]
                    mean[j] = mean[j - 1] + j * histogram[j]
                }

                totalMean = mean[l - 1]

                // Calculate the inter-class variance and find the threshold that maximizes it.
                var threshold = 0
                maxInterClassVariance = 0.0

                for (i in 0 until l) {
                    interClassVariance = totalMean * cumulativeHistogram[i] - mean[i]
                    interClassVariance *= interClassVariance / (cumulativeHistogram[i] * (1.0 - cumulativeHistogram[i]))

                    if (maxInterClassVariance < interClassVariance) {
                        maxInterClassVariance = interClassVariance
                        threshold = i
                    }
                }
                thresholdedPixels[position] =
                    if ((pixels[position] and 0xff.toByte()).toInt() > threshold ||
                        (pixels[position] and 0xff.toByte()).toInt() == 255) {
                        0xff.toByte()
                    } else {
                        0.toByte()
                    }
            }
        }
        for (i in 0 until width * height) {
            pixels[i] = thresholdedPixels[i]
        }
        imageProcessor.pixels = thresholdedPixels
        image.processor = imageProcessor
    }

    fun bernsen(image: ImagePlus, radius: Int, contrastThreshold: Double) {
        ImageConverter(image).convertToGray8()
        val imageProcessor = image.processor
        val rankFilters = RankFilters()

        val maxImage = duplicateImage(imageProcessor)
        val maxImageProcessor = maxImage.processor
        rankFilters.rank(maxImageProcessor, radius.toDouble(), RankFilters.MAX) // Maximum

        val minImage = duplicateImage(imageProcessor)
        val minImageProcessor = minImage.processor
        rankFilters.rank(minImageProcessor, radius.toDouble(), RankFilters.MIN) // Minimum

        val pixels = imageProcessor.pixels as ByteArray
        val maxPixels = maxImageProcessor.pixels as ByteArray
        val minPixels = minImageProcessor.pixels as ByteArray

        var localContrast: Int
        var midGray: Int
        var temp: Int
        for (i in pixels.indices) {
            localContrast = (maxPixels[i] and 0xff.toByte()) - (minPixels[i] and 0xff.toByte())
            midGray = ((minPixels[i] and 0xff.toByte()) + (maxPixels[i] and 0xff.toByte())) / 2
            temp = (pixels[i] and 0x0000ff.toByte()).toInt()
            if (localContrast < contrastThreshold)
                pixels[i] = if (midGray >= 128) 0xff.toByte() else 0.toByte()
            else
                pixels[i] = if (temp >= midGray) 0xff.toByte() else 0.toByte()
        }
        imageProcessor.pixels = pixels
        image.processor = imageProcessor
    }

    fun niblack(image: ImagePlus, radius: Int, kValue: Double, cValue: Double) {
        ImageConverter(image).convertToGray8()
        val imageProcessor = image.processor

        val rankFilters = RankFilters()

        // Mean
        val meanImage = duplicateImage(imageProcessor)
        var ic = ImageConverter(meanImage)
        ic.convertToGray32()
        val meanImageProcessor = meanImage.processor
        rankFilters.rank(meanImageProcessor, radius.toDouble(), RankFilters.MEAN)

        // Variance
        val varImage = duplicateImage(imageProcessor)
        ic = ImageConverter(varImage)
        ic.convertToGray32()
        val varImageProcessor = varImage.processor
        rankFilters.rank(varImageProcessor, radius.toDouble(), RankFilters.VARIANCE)

        val pixels = imageProcessor.pixels as ByteArray
        val mean = meanImageProcessor.pixels as FloatArray
        val variance = varImageProcessor.pixels as FloatArray

        for (i in pixels.indices)
            pixels[i] =
                if ((pixels[i] and 0xff.toByte()).toInt() >
                    (mean[i] + kValue * sqrt(variance[i].toDouble()) - cValue).toInt()) {
                    0xff.toByte()
                } else {
                    0.toByte()
                }
        imageProcessor.pixels = pixels
        image.processor = imageProcessor
        return
    }

    fun thresholdImage(image: ImagePlus, thresholdChoice: String, thresholdAlgo: String, localThresholdRadius: Int) {
        when (thresholdChoice) {
            "Global" -> {
                image.channelProcessor.autoThreshold()
                when (thresholdAlgo) {
                    "Otsu's" -> return
                    "Bernsen's" -> return
                    "Niblack's" -> return
                    else -> throw InvalidArgumentException("Threshold Algorithm selected")
                }
            }
            "Local" -> {
                when (thresholdAlgo) {
                    "Otsu's" -> otsu(image, localThresholdRadius)
                    "Bernsen's" -> bernsen(image, localThresholdRadius, 15.0) // Not sure what additional param ought to be
                    "Niblack's" -> niblack(image, localThresholdRadius, 0.2, 0.0) // Not sure what additional params ought to be
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
