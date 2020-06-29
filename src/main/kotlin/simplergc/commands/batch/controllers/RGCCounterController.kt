package simplergc.commands.batch.controllers

import simplergc.commands.batch.BatchableCellCounter
import java.awt.event.ActionListener
import java.io.FileNotFoundException
import simplergc.commands.batch.RGCBatch.OutputFormat
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.models.RGCCounterParameters
import simplergc.commands.batch.views.RGCCounterView
import simplergc.services.CellDiameterRange

class RGCCounterController(val view: RGCCounterView, private val model: RGCCounterModel) : RGCController() {
    init {
        view.addListeners(this)
    }

    private fun harvestParameters(): RGCCounterParameters {
        return RGCCounterParameters(
            view.inputDirectoryChooser.directory,
            view.shouldProcessFilesInNestedFoldersCheckbox.isSelected,
            view.channelSpinner.value,
            view.thresholdRadiusSpinner.value,
            view.gaussianBlurSpinner.value.toDouble(),
            view.shouldRemoveAxonsCheckbox.isSelected,
            CellDiameterRange.parseFromText(view.cellDiameterChannelField.field.text),
            view.outputFileChooserPanel.format,
            view.outputFileChooserPanel.file,
            model.context
        )
    }

    private fun saveParameters(p: RGCCounterParameters) {
        model.shouldProcessFilesInNestedFolders = p.shouldProcessFilesInNestedFolders
        model.channelToUse = p.channel
        model.thresholdRadius = p.thresholdRadius
        model.gaussianBlur = p.gaussianBlurSigma
        model.cellDiameter = view.cellDiameterChannelField.field.text
        model.saveAsCSV = view.outputFileChooserPanel.format == OutputFormat.CSV
        model.outputFile = view.outputFileChooserPanel.file.absolutePath
    }

    private fun process(p: RGCCounterParameters) {
        if (p.inputDirectory == null) {
            throw FileNotFoundException("No input directory is selected")
        } else if (p.outputFile == null) {
            throw FileNotFoundException("No output file selected")
        } else if (!p.inputDirectory.exists()) {
            throw FileNotFoundException("The input folder could not be opened. Please create it if it does not already exist")
        }

        val files = getAllFiles(p.inputDirectory, p.shouldProcessFilesInNestedFolders)
        val cellCounter = BatchableCellCounter(p.channel, p.shouldRemoveAxons, p.context)

        cellCounter.process(
            openFiles(files),
            p.cellDiameterRange,
            p.thresholdRadius,
            p.gaussianBlurSigma,
            p.outputFormat,
            p.outputFile
        )
    }

    override fun okButton(): ActionListener {
        return ActionListener {
            val p = harvestParameters()
            saveParameters(p)


            try {
                process(p)

                view.dialog("Saved", "The batch processing results have successfully been saved to the specified file")
            } catch (e: FileNotFoundException) {
                view.dialog("Error", e.message ?: "An error occurred")
            }
        }
    }
}
