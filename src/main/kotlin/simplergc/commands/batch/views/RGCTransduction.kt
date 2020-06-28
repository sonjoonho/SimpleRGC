package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.io.FileNotFoundException
import java.util.prefs.Preferences
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SpinnerNumberModel
import org.scijava.Context
import simplergc.commands.batch.controllers.runRGCTransduction
import simplergc.commands.batch.getRGCTransductionPref
import simplergc.commands.batch.putRGCTransductionPref
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.addCellDiameterField
import simplergc.commands.batch.views.common.addCheckBox
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.addSpinner
import simplergc.services.CellDiameterRange

/** Creates the Simple Colocalizer GUI. */
fun rgcTransductionPanel(context: Context, prefs: Preferences): JPanel {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val inputDirectoryChooser = InputDirectoryChooserPanel(container, prefs)
    container.add(inputDirectoryChooser)

    val shouldProcessFilesInNestedFoldersCheckbox =
        addCheckBox(container, "Batch process in nested sub-folders?", prefs, "shouldProcessFilesInNestedFolders", false)

    addMessage(container, "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>")

    val morphologyChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("morphologyChannel", 1), 1, 100, 1)
    val targetChannelSpinner = addSpinner(container, "Cell morphology channel", morphologyChannelModel)
    targetChannelSpinner.toolTipText = "Used as minimum/maximum diameter when identifying cells"

    val transductionChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("transductionChannel", 2), 1, 100, 1)
    val transducedChannelSpinner = addSpinner(container, "Transduction channel", transductionChannelModel)

    addMessage(container, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(container, prefs, "cellDiameter", false)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCTransductionPref("thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(container, "Local threshold radius", thresholdRadiusModel)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCTransductionPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(container, "Gaussian blur sigma", gaussianBlurModel)
    gaussianBlurSpinner.toolTipText = "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    val outputFileChooserPanel = OutputFileChooserPanel(container, prefs)
    container.add(outputFileChooserPanel)

    val okButton = JButton("Ok")
    container.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCTransductionPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        prefs.putRGCTransductionPref("thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        prefs.putRGCTransductionPref("gaussianBlur", gaussianBlurSigma)
        val targetChannel = targetChannelSpinner.value as Int
        prefs.putRGCTransductionPref("morphologyChannel", targetChannel)
        val transducedChannel = transducedChannelSpinner.value as Int
        prefs.putRGCTransductionPref("transductionChannel", transducedChannel)

        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
        prefs.putRGCTransductionPref("cellDiameter", cellDiameterChannelField.text)

        try {
            runRGCTransduction(
                inputDirectoryChooser.directory,
                shouldProcessFilesInNestedFolders,
                thresholdRadius,
                gaussianBlurSigma,
                targetChannel,
                transducedChannel,
                cellDiameterRange,
                outputFileChooserPanel.file,
                outputFileChooserPanel.format,
                context
            )
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The batch processing results have successfully been saved to the specified file."
            )
        } catch (e: FileNotFoundException) {
            MessageDialog(IJ.getInstance(), "Error", e.message)
        }
    }
    return container
}
