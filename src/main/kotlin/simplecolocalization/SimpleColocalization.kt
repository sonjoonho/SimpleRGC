package simplecolocalization

import ij.ImagePlus
import java.io.File
import net.imagej.ImageJ
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

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
        image.show()
        image.channelProcessor.autoThreshold()
        image.show()
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
