package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.awt.GridLayout
import java.io.FileNotFoundException
import java.util.prefs.Preferences
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SpinnerNumberModel
import org.scijava.Context
import simplergc.commands.batch.RGCBatch
import simplergc.commands.batch.controllers.runRGCCounter
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.addCellDiameterField
import simplergc.commands.batch.views.common.addCheckBox
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.addSpinner
import simplergc.services.CellDiameterRange

/** Creates the RGC Counter GUI. */
fun rgcCounterPanel(context: Context, prefs: Preferences): JPanel {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val inputDirectoryChooser = InputDirectoryChooserPanel(container, prefs)
    container.add(inputDirectoryChooser)

    val shouldProcessFilesInNestedFoldersCheckbox =
        addCheckBox(container, "Batch process in nested sub-folders?", prefs, "shouldProcessFilesInNestedFolders", true)

    val channelModel = SpinnerNumberModel(prefs.getRGCCounterPref("channelToUse", 1), 0, 10, 1)
    val channelSpinner = addSpinner(container, "Select channel to use", channelModel)

    addMessage(container, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(container, prefs, "cellDiameter", true)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCCounterPref("thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(container, "Local threshold radius", thresholdRadiusModel)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCCounterPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(container, "Gaussian blur sigma", gaussianBlurModel)
    gaussianBlurSpinner.toolTipText =
        "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    val shouldRemoveAxonsCheckbox = addCheckBox(container, "Remove Axons", prefs, "shouldRemoveAxons", true)

    val outputFileChooserPanel = OutputFileChooserPanel(container, prefs)
    container.add(outputFileChooserPanel)

    val okButton = JButton("Ok")
    container.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCCounterPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val channel = channelSpinner.value as Int
        prefs.putRGCCounterPref("channelToUse", channel)
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        prefs.putRGCCounterPref("thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        prefs.putRGCCounterPref("gaussianBlur", gaussianBlurSigma)
        val shouldRemoveAxons = shouldRemoveAxonsCheckbox.isSelected
        prefs.putRGCCounterPref("shouldRemoveAxons", shouldRemoveAxons)
        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
        prefs.putRGCCounterPref("cellDiameter", cellDiameterChannelField.text)

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
                context
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

    container.add(JPanel())
    return container
}
