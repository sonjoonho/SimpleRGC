package simplecolocalization

import ij.ImagePlus
import ij.io.Opener
import ij.process.ByteProcessor
import java.io.File
import java.lang.Integer.min
import loci.formats.`in`.LIFReader

class ImageJOpener {

    companion object {

        /** Identify whether file contains multiple images */
        fun isStack(inputFile: File): Boolean {
            return when {
                inputFile.extension == "lif" -> true
                inputFile.extension == "tiff" -> true
                inputFile.extension == "tif" -> true
                else -> false
            }
        }
    }

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

    /** Function to be used for opening LIF and TIFF/TIF files. NUMSLICES refers to the number of slices requested
     *  by the user. */
    fun openStack(file: File, numSlices: Int): List<ImagePlus> {
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
                throw UnsupportedFileTypeException(".$extension file extension is unsupported")
            }
        }
    }

    fun openSingleImage(file: File): ImagePlus {
        init(file)
        return opener.openImage(absolutePath)
            ?: throw UnsupportedFileTypeException(".$extension file extension is unsupported")
    }
}
