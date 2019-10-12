package simplecolocalization

import ij.ImagePlus
import ij.plugin.filter.BackgroundSubtracter
import ij.plugin.filter.EDM
import ij.plugin.filter.RankFilters
import java.io.File
import net.imagej.ImageJ
import ij.process.ImageConverter
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

//TODO (#5): Figure out what this value should be
const val LARGEST_CELL_DIAMETER = 30.0

/**
 * This provides basic scaffolding for an ImageJ plugin.
 *
 */
@Plugin(type = Command::class, menuPath = "Plugins > Simple Colocalization")
class SimpleColocalization : Command {

    @Parameter
    private lateinit var imageFile: File

    override fun run() {
        val image = ImagePlus(imageFile.absolutePath)
        preprocessImage(image)
        watershedImage(image)
        image.show()
    }

    private fun preprocessImage(image: ImagePlus) {
        // Convert to grayscale 8-bit
        val imageConverter = ImageConverter(image)
        imageConverter.convertToGray8()

        // Remove background
        val backgroundSubtracter = BackgroundSubtracter()
        backgroundSubtracter.rollingBallBackground(image.channelProcessor,
            LARGEST_CELL_DIAMETER,
            false,
            false,
            false,
            false,
            false)

        // Despeckle image
        val rankFilters = RankFilters()
        rankFilters.rank(image.channelProcessor, 1.0, RankFilters.MEDIAN)

        // Threshold image
        image.channelProcessor.autoThreshold()
    }

    private fun watershedImage(image: ImagePlus) {
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
