package simplecolocalization.commands.batch

import ij.ImagePlus
import java.io.File

interface Batchable {
    fun process(
        inputImages: List<ImagePlus>,
        largestCellDiameter: Double,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    )
}
