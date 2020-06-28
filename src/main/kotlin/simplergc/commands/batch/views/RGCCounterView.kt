package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.io.FileNotFoundException
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SpinnerNumberModel
import simplergc.commands.batch.controllers.runRGCCounter
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.addCellDiameterField
import simplergc.commands.batch.views.common.addCheckBox
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.addSpinner
import simplergc.services.CellDiameterRange

class RGCCounterView(model: RGCCounterModel) : JPanel() {
    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)

        val inputDirectoryChooser = InputDirectoryChooserPanel(this, model.prefs)
        this.add(inputDirectoryChooser)

        val shouldProcessFilesInNestedFoldersCheckbox =
            addCheckBox(
                this,
                "Batch process in nested sub-folders?",
                model.prefs,
                "shouldProcessFilesInNestedFolders",
                true
            )

        val channelModel = SpinnerNumberModel(model.prefs.getRGCCounterPref("channelToUse", 1), 0, 10, 1)
        val channelSpinner = addSpinner(this, "Select channel to use", channelModel)

        addMessage(this, "Image processing parameters")

        val cellDiameterChannelField = addCellDiameterField(this, model.prefs, "cellDiameter", true)

        val thresholdRadiusModel = SpinnerNumberModel(model.prefs.getRGCCounterPref("thresholdRadius", 20), 1, 1000, 1)
        val thresholdRadiusSpinner = addSpinner(this, "Local threshold radius", thresholdRadiusModel)
        thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

        val gaussianBlurModel = SpinnerNumberModel(model.prefs.getRGCCounterPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
        val gaussianBlurSpinner = addSpinner(this, "Gaussian blur sigma", gaussianBlurModel)
        gaussianBlurSpinner.toolTipText =
            "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

        val shouldRemoveAxonsCheckbox = addCheckBox(this, "Remove Axons", model.prefs, "shouldRemoveAxons", true)

        val outputFileChooserPanel = OutputFileChooserPanel(this, model.prefs)
        this.add(outputFileChooserPanel)

        val okButton = JButton("Ok")
        this.add(okButton)
        okButton.addActionListener {
            val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
            model.prefs.putRGCCounterPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
            val channel = channelSpinner.value as Int
            model.prefs.putRGCCounterPref("channelToUse", channel)
            val thresholdRadius = thresholdRadiusSpinner.value as Int
            model.prefs.putRGCCounterPref("thresholdRadius", thresholdRadius)
            val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
            model.prefs.putRGCCounterPref("gaussianBlur", gaussianBlurSigma)
            val shouldRemoveAxons = shouldRemoveAxonsCheckbox.isSelected
            model.prefs.putRGCCounterPref("shouldRemoveAxons", shouldRemoveAxons)
            val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
            model.prefs.putRGCCounterPref("cellDiameter", cellDiameterChannelField.text)

            try {
                runRGCCounter(
                    inputDirectoryChooser.directory,
                    shouldProcessFilesInNestedFolders,
                    channel,
                    thresholdRadius,
                    gaussianBlurSigma,
                    shouldRemoveAxons,
                    cellDiameterRange,
                    outputFileChooserPanel.format,
                    outputFileChooserPanel.file,
                    model.context
                )
                MessageDialog(
                    IJ.getInstance(),
                    "Saved",
                    "The batch processing results have successfully been saved to the specified file"
                )
            } catch (e: FileNotFoundException) {
                MessageDialog(IJ.getInstance(), "Error", e.message)
            }
        }
    }
}
