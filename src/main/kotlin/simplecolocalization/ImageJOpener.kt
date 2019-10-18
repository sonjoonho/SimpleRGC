package simplecolocalization

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.io.Opener
import ij.process.ByteProcessor
import java.io.File
import java.lang.Integer.min
import loci.formats.`in`.LIFReader

class ImageJOpener {

    private val opener = Opener()
    private val lifReader = LIFReader()
    private lateinit var extension: String
    private lateinit var absolutePath: String
    private var isLif: Boolean = false
    private var isTiff: Boolean = false

    private fun init(file: File) {
        extension = file.extension
        absolutePath = file.absolutePath
        if (extension == "lif") isLif = true
        if (extension == "tiff" || extension == "tif") isTiff = true
    }

    fun openStack(file: File, numSlices: Int): List<ImagePlus>? {
        init(file)
        var result = mutableListOf<ImagePlus>()
        return when {
            isLif -> {
                // Use BioFormats LIFReader to open LIF series.
                lifReader.setId(absolutePath)
                val count = lifReader.seriesCount
                for (i in 0 until min(numSlices, count)) {
                    lifReader.series = i
                    val bytes = ByteProcessor(lifReader.sizeX, lifReader.sizeY, lifReader.openBytes(0))
                    result.add(ImagePlus("image", bytes))
                }
                result
            }
            isTiff -> {
                // Use ImageJ Opener to open TIFF stack.
                val size = Opener.getTiffFileInfo(absolutePath).size
                for (i in 1..min(numSlices, size)) {
                    result.add(opener.openTiff(absolutePath, i))
                }
                result
            }
            else -> {
                // Use ImageJ Opener to open other common image types.
                MessageDialog(IJ.getInstance(), "Error", "Unsupported file type!")
                return null
            }
        }
    }

    fun openSingleImage(file: File): ImagePlus? {
        init(file)
        val image = opener.openImage(absolutePath)
        if (image == null) {
            MessageDialog(IJ.getInstance(), "Error", "Unsupported file type: $extension")
            return null
        }
        return image
    }
}
