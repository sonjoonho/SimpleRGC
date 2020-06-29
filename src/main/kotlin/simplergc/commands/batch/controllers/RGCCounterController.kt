package simplergc.commands.batch.controllers

import simplergc.commands.batch.Batchable
import simplergc.commands.batch.BatchableCellCounter
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.models.RGCCounterParameters
import simplergc.commands.batch.models.RGCParameters
import simplergc.commands.batch.views.RGCCounterView
import simplergc.services.CellDiameterRange

class RGCCounterController(override val view: RGCCounterView, private val model: RGCCounterModel) : RGCController() {
    init {
        view.addListeners(this)
    }

    override fun harvestParameters(): RGCCounterParameters {
        return RGCCounterParameters(
            view.inputDirectoryChooser.directory,
            view.shouldProcessFilesInNestedFoldersCheckbox.isSelected,
            view.thresholdRadiusSpinner.value,
            view.channelSpinner.value,
            view.gaussianBlurSpinner.value.toDouble(),
            view.shouldRemoveAxonsCheckbox.isSelected,
            CellDiameterRange.parseFromText(view.cellDiameterChannelField.field.text),
            view.outputFileChooserPanel.format,
            view.outputFileChooserPanel.file,
            model.context
        )
    }

    override fun saveParameters(p: RGCParameters) {
        p as RGCCounterParameters
        model.shouldProcessFilesInNestedFolders = p.shouldProcessFilesInNestedFolders
        model.channelToUse = p.channel
        model.thresholdRadius = p.thresholdRadius
        model.gaussianBlur = p.gaussianBlurSigma
        model.cellDiameter = view.cellDiameterChannelField.field.text
        model.saveAsCSV = view.outputFileChooserPanel.format == OutputFormat.CSV
        model.outputFile = view.outputFileChooserPanel.file.absolutePath
    }

    override fun makeProcessor(p: RGCParameters): Batchable {
        p as RGCCounterParameters
        return BatchableCellCounter(p.channel, p.shouldRemoveAxons, p.context)
    }
}
