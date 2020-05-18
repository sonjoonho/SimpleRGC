package simplecolocalization.commands.batch

import ij.ImagePlus
import java.io.File

interface Batchable {
    fun process(
        inputImages: List<ImagePlus>,
        smallestCellDiameter: Double,
        largestCellDiameter: Double,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    )
}
