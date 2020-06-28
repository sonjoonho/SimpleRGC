package simplergc.commands.batch.controllers

import java.awt.event.ActionListener
import java.io.FileNotFoundException
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.views.RGCCounterView
import simplergc.services.CellDiameterRange
import simplergc.services.OutputFormat

class RGCCounterController(val view: RGCCounterView, private val model: RGCCounterModel) {
    init {
        view.addListeners(this)
    }

    fun okButton(): ActionListener {
        return ActionListener {
            val shouldProcessFilesInNestedFolders = view.shouldProcessFilesInNestedFoldersCheckbox.isSelected
            model.shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFolders
            val channel = view.channelSpinner.value
            model.channelToUse = channel
            val thresholdRadius = view.thresholdRadiusSpinner.value
            model.thresholdRadius = thresholdRadius
            val gaussianBlurSigma = (view.gaussianBlurSpinner.value).toDouble()
            model.gaussianBlur = gaussianBlurSigma
            val shouldRemoveAxons = view.shouldRemoveAxonsCheckbox.isSelected
            val cellDiameterRange = CellDiameterRange.parseFromText(view.cellDiameterChannelField.field.text)
            model.cellDiameter = view.cellDiameterChannelField.field.text

            model.saveAsCSV = view.outputFileChooserPanel.format == OutputFormat.CSV
            model.saveAsXML = view.outputFileChooserPanel.format == OutputFormat.XML

            model.outputFile = view.outputFileChooserPanel.file.absolutePath

            println(
                listOf(
                    "Running with ", view.inputDirectoryChooser.directory,
                    shouldProcessFilesInNestedFolders,
                    channel,
                    thresholdRadius,
                    gaussianBlurSigma,
                    shouldRemoveAxons,
                    cellDiameterRange,
                    view.outputFileChooserPanel.format,
                    view.outputFileChooserPanel.file,
                    model.context
                )
            )

            try {
                runRGCCounter(
                    view.inputDirectoryChooser.directory,
                    shouldProcessFilesInNestedFolders,
                    channel,
                    thresholdRadius,
                    gaussianBlurSigma,
                    shouldRemoveAxons,
                    cellDiameterRange,
                    view.outputFileChooserPanel.format,
                    view.outputFileChooserPanel.file,
                    model.context
                )
                view.error("Saved", "The batch processing results have successfully been saved to the specified file")
            } catch (e: FileNotFoundException) {
                view.error("Error", e.message ?: "An error occurred")
            }
        }
    }
}
