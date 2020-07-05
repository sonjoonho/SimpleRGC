package simplergc.commands.batch.models

import java.io.File
import org.scijava.Context
import simplergc.services.CellDiameterRange

// Alright so I may have gotten slightly lazy with the design patterns here but you know what it works well and it's
// definitely better than before. Just don't look at this file. What do you want from me.
abstract class RGCParameters(
    open val inputDirectory: File?,
    open val shouldProcessFilesInNestedFolders: Boolean,
    open val gaussianBlurSigma: Double,
    open val thresholdRadius: Int,
    open val cellDiameterRange: CellDiameterRange,
    open val outputFormat: String,
    open val outputFile: File?,
    open val context: Context
)

data class RGCCounterParameters(
    override val inputDirectory: File?,
    override val shouldProcessFilesInNestedFolders: Boolean,
    val channel: Int,
    override val thresholdRadius: Int,
    override val gaussianBlurSigma: Double,
    val shouldRemoveAxons: Boolean,
    override val cellDiameterRange: CellDiameterRange,
    override val outputFormat: String,
    override val outputFile: File?,
    override val context: Context
) : RGCParameters(
    inputDirectory,
    shouldProcessFilesInNestedFolders,
    gaussianBlurSigma,
    thresholdRadius,
    cellDiameterRange,
    outputFormat,
    outputFile,
    context
)

data class RGCTransductionParameters(
    override val inputDirectory: File?,
    override val shouldProcessFilesInNestedFolders: Boolean,
    override val thresholdRadius: Int,
    override val gaussianBlurSigma: Double,
    val targetChannel: Int,
    val shouldRemoveAxonsFromTargetChannel: Boolean,
    val transductionChannel: Int,
    val shouldRemoveAxonsFromTransductionChannel: Boolean,
    override val cellDiameterRange: CellDiameterRange,
    override val outputFormat: String,
    override val outputFile: File?,
    override val context: Context
) : RGCParameters(
    inputDirectory,
    shouldProcessFilesInNestedFolders,
    gaussianBlurSigma,
    thresholdRadius,
    cellDiameterRange,
    outputFormat,
    outputFile,
    context
)
