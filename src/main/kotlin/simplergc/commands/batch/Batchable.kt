package simplergc.commands.batch

import ij.ImagePlus
import java.io.File
import simplergc.services.CellDiameterRange

interface Batchable {
    fun process(
        inputImages: List<ImagePlus>,
        cellDiameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File
    )
}
