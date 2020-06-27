package simplecolocalization.commands.batch

import ij.ImagePlus
import java.io.File
import simplecolocalization.services.CellDiameterRange

interface Batchable {
    fun process(
        inputImages: List<ImagePlus>,
        cellDiameterRange: CellDiameterRange,
        localThresholdRadius: Int,
        gaussianBlurSigma: Double,
        shouldRemoveAxons: Boolean,
        outputFormat: String,
        outputFile: File
    )
}
