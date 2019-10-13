package simplecolocalization

import ij.ImagePlus
import ij.gui.PointRoi
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.GaussianBlur
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.RankFilters
import ij.process.ImageConverter
import java.io.File
import net.imagej.ImageJ
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

// TODO (#5): Figure out what this value should be
const val LARGEST_CELL_DIAMETER = 30.0

// Defined in ImageJ docs: Despeckle is a median filter with radius 1.0
const val DESPECKLE_RADIUS = 1.0

// TODO: Justify this values or make them parameters
const val GAUSSIAN_SIGMA = 3.0

/**
 * This provides basic scaffolding for an ImageJ plugin.
 *
 */
@Plugin(type = Command::class, menuPath = "Plugins > Simple Colocalization")
class SimpleColocalization : Command {

    @Parameter
    private lateinit var imageFile: File

    override fun run() {
        val originalImage = ImagePlus(imageFile.absolutePath)
        val image = ImagePlus(imageFile.absolutePath)
        preprocessImage(image)
        segmentImage(image)
        image.show()
    }

    /**
     * Perform pre-processing on the image to remove background and set cells to white
     */
    private fun preprocessImage(image: ImagePlus) {
        // Convert to grayscale 8-bit
        val imageConverter = ImageConverter(image)
        imageConverter.convertToGray8()

        // Remove background
        val backgroundSubtracter = BackgroundSubtracter()
        backgroundSubtracter.rollingBallBackground(
            image.channelProcessor,
            LARGEST_CELL_DIAMETER,
            false,
            false,
            false,
            false,
            false
        )
        // Threshold image
        image.channelProcessor.autoThreshold()

        // Despeckle image
        val rankFilters = RankFilters()
        rankFilters.rank(image.channelProcessor, DESPECKLE_RADIUS, RankFilters.MEDIAN)

        // Apply Gaussian Blur to group larger speckles
        val gaussianBlur = GaussianBlur()
        gaussianBlur.blurGaussian(image.channelProcessor, GAUSSIAN_SIGMA)

        // Threshold image
        image.channelProcessor.autoThreshold()
    }

    /**
     * Segment the image into individual cells, overlaying outlines for cells in the image
     *
     * Uses ImageJ's Euclidean Distance Map plugin for performing the watershed algorithm.
     * Used as a simple starting point that'd allow for cell counting.
     */
    private fun segmentImage(image: ImagePlus) {
        // TODO (#7): Review and improve upon simple watershed
        val edm = EDM()
        edm.toWatershed(image.channelProcessor)
    }

    companion object {

        /**
         * This main function serves for development purposes.
         * It allows you to run the plugin immediately out of
         * your integrated development environment (IDE).
         * @param args whatever, it's ignored
         * *
         * @throws Exception
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            // create the ImageJ application context with all available services
            val ij = ImageJ()
            ij.ui().showUI()

            ij.command().run(SimpleColocalization::class.java, true)
        }
    }
}
