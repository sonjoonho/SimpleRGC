package simplergc.commands.batch.models

object Param {
    // Counter parameters
    const val channelToUse = "channelToUse"
    const val shouldRemoveAxons = "shouldRemoveAxons"

    // Transduction parameters
    const val inputDirectory = "inputDirectory"
    const val targetChannel = "targetChannel"
    const val transductionChannel = "transductionChannel"
    const val shouldRemoveAxonsFromTargetChannel = "shouldRemoveAxonsFromTargetChannel"
    const val shouldRemoveAxonsFromTransductionChannel = "shouldRemoveAxonsTransductionChannel"

    // Common
    const val shouldProcessFilesInNestedFolders = "shouldProcessFilesInNestedFolders"
    const val cellDiameter = "cellDiameter"
    const val thresholdRadius = "thresholdRadius"
    const val gaussianBlur = "gaussianBlur"
    const val saveAsCSV = "saveAsCSV"
    const val saveAsXML = "saveAsXML"
    const val outputFile = "outputFile"
}
