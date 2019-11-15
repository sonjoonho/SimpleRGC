package simplecolocalization

import ij.IJ
import ij.ImagePlus
import ij.gui.NewImage
import ij.gui.OvalRoi
import ij.plugin.filter.RankFilters
import ij.process.Blitter
import ij.process.ImageConverter
import ij.process.ImageProcessor
import kotlin.experimental.and
import kotlin.math.sqrt

/** Copied with modification from FIJI's auto local thresholding algorithms: https://github.com/fiji/Auto_Local_Threshold */
private fun duplicateImage(iProcessor: ImageProcessor): ImagePlus {
    val w = iProcessor.width
    val h = iProcessor.height
    val iPlus = NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK)
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

    val roi = OvalRoi(0, 0, 2 * radius, 2 * radius)
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
                    (pixels[position] and 0xff.toByte()).toInt() == 255
                ) {
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
                (mean[i] + kValue * sqrt(variance[i].toDouble()) - cValue).toInt()
            ) {
                0xff.toByte()
            } else {
                0.toByte()
            }
    imageProcessor.pixels = pixels
    image.processor = imageProcessor
    return
}
