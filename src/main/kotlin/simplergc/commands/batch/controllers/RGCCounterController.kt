package simplergc.commands.batch.controllers

import java.awt.event.ActionListener
import java.io.FileNotFoundException
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.views.RGCCounterView
import simplergc.services.CellDiameterRange

class RGCCounterController(val view: RGCCounterView, private val model: RGCCounterModel) {
    // TODO(sonjoonho): Unimplemented.

    init {
        view.addListeners(this)
    }

    fun okButton(): ActionListener {
        return ActionListener {
            val shouldProcessFilesInNestedFolders = view.shouldProcessFilesInNestedFoldersCheckbox.isSelected
            model.prefs.putRGCCounterPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
            val channel = view.channelSpinner.value as Int
            model.prefs.putRGCCounterPref("channelToUse", channel)
            val thresholdRadius = view.thresholdRadiusSpinner.value
            model.prefs.putRGCCounterPref("thresholdRadius", thresholdRadius)
            val gaussianBlurSigma = (view.gaussianBlurSpinner.value).toDouble()
            model.prefs.putRGCCounterPref("gaussianBlur", gaussianBlurSigma)
            val shouldRemoveAxons = view.shouldRemoveAxonsCheckbox.isSelected
            model.prefs.putRGCCounterPref("shouldRemoveAxons", shouldRemoveAxons)
            val cellDiameterRange = CellDiameterRange.parseFromText(view.cellDiameterChannelField.field.text)
            model.prefs.putRGCCounterPref("cellDiameter", view.cellDiameterChannelField.field.text as String)

            println(listOf(
                "Running with ", view.inputDirectoryChooser.directory,
                shouldProcessFilesInNestedFolders,
                channel,
                thresholdRadius,
                gaussianBlurSigma,
                shouldRemoveAxons,
                cellDiameterRange,
                view.outputFileChooserPanel.format,
                view.outputFileChooserPanel.file,
                model.context)
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
