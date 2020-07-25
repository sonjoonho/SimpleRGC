package simplergc.commands.batch.controllers

import simplergc.commands.batch.Batchable
import simplergc.commands.batch.BatchableColocalizer
import simplergc.commands.batch.models.RGCParameters
import simplergc.commands.batch.models.RGCTransductionModel
import simplergc.commands.batch.models.RGCTransductionParameters
import simplergc.commands.batch.views.RGCTransductionView
import simplergc.services.CellDiameterRange

class RGCTransductionController(override val view: RGCTransductionView, private val model: RGCTransductionModel) :
    RGCController() {
    init {
        view.addListeners(this)
    }

    override fun harvestParameters(): RGCTransductionParameters {
        return RGCTransductionParameters(
            view.inputDirectoryChooser.directory,
            view.shouldProcessFilesInNestedFoldersCheckbox.isSelected,
            view.thresholdRadiusSpinner.value,
            view.gaussianBlurSpinner.value.toDouble(),
            view.targetChannelSpinner.value,
            view.shouldRemoveAxonsFromTargetChannelCheckbox.isSelected,
            view.transductionChannelSpinner.value,
            view.shouldRemoveAxonsFromTransductionChannelCheckbox.isSelected,
            CellDiameterRange.parseFromText(view.cellDiameterField.field.text),
            view.outputFileChooserPanel.format,
            view.outputFileChooserPanel.file,
            model.context
        )
    }

    override fun saveParameters(p: RGCParameters) {
        p as RGCTransductionParameters
        model.inputDirectory = p.inputDirectory?.path ?: ""
        model.shouldProcessFilesInNestedFolders = p.shouldProcessFilesInNestedFolders
        model.thresholdRadius = p.thresholdRadius
        model.gaussianBlur = p.gaussianBlurSigma
        model.targetChannel = p.targetChannel
        model.shouldRemoveAxonsFromTargetChannel = p.shouldRemoveAxonsFromTargetChannel
        model.transductionChannel = p.transductionChannel
        model.cellDiameter = view.cellDiameterField.field.text
        model.shouldRemoveAxonsFromTransductionChannel = p.shouldRemoveAxonsFromTransductionChannel
        model.outputFormat = view.outputFileChooserPanel.format
        model.outputFile = view.outputFileChooserPanel.file.absolutePath
    }

    override fun makeProcessor(p: RGCParameters): Batchable {
        p as RGCTransductionParameters
        return BatchableColocalizer(
            p.targetChannel,
            p.shouldRemoveAxonsFromTargetChannel,
            p.transductionChannel,
            p.shouldRemoveAxonsFromTransductionChannel,
            p.context
        )
    }
}
