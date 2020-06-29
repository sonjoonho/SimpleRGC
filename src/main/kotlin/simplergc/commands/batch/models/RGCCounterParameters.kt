package simplergc.commands.batch.models

import org.scijava.Context
import simplergc.services.CellDiameterRange
import java.io.File

data class RGCCounterParameters(
    val inputDirectory: File?,
    val shouldProcessFilesInNestedFolders: Boolean,
    val channel: Int,
    val thresholdRadius: Int,
    val gaussianBlurSigma: Double,
    val shouldRemoveAxons: Boolean,
    val cellDiameterRange: CellDiameterRange,
    val outputFormat: String,
    val outputFile: File?,
    val context: Context
)