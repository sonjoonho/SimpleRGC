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
import simplergc.commands.batch.views.common.CellDiameterField
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.RGCSpinner
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.RGCCheckbox
import simplergc.services.CellDiameterRange

/** Creates the Simple Colocalizer GUI. */
fun rgcTransductionPanel(context: Context, prefs: Preferences): JPanel {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val inputDirectoryChooser = InputDirectoryChooserPanel(container, prefs)
    container.add(inputDirectoryChooser)

    val shouldProcessFilesInNestedFoldersCheckbox =
        RGCCheckbox(
            "Batch process in nested sub-folders?",
            prefs,
            "shouldProcessFilesInNestedFolders",
            false
        )
    container.add(shouldProcessFilesInNestedFoldersCheckbox)

    addMessage(
        container,
        "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>"
    )

    val morphologyChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("morphologyChannel", 1), 1, 100, 1)
    val targetChannelSpinner = RGCSpinner("Cell morphology channel", morphologyChannelModel)
    container.add(targetChannelSpinner)
    targetChannelSpinner.toolTipText = "Used as minimum/maximum diameter when identifying cells"
    val shouldRemoveAxonsFromTargetChannelCheckbox =
        RGCCheckbox("Exclude Axons from target channel", prefs, "shouldRemoveAxonsFromTargetChannel", true)

    val transductionChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("transductionChannel", 2), 1, 100, 1)
    val transducedChannelSpinner = RGCSpinner("Transduction channel", transductionChannelModel)
    container.add(transducedChannelSpinner)
    val shouldRemoveAxonsFromTransductionChannelCheckbox = RGCCheckbox(
        "Exclude Axons from Transduction channel",
        prefs,
        "shouldRemoveAxonsFromTransductionChannel",
        true
    )

    addMessage(container, "Image processing parameters")

    val cellDiameterChannelField = CellDiameterField(prefs, "cellDiameter", false)
    container.add(cellDiameterChannelField)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCTransductionPref("thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = RGCSpinner("Local threshold radius", thresholdRadiusModel)
    container.add(thresholdRadiusSpinner)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCTransductionPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = RGCSpinner("Gaussian blur sigma", gaussianBlurModel)
    container.add(gaussianBlurSpinner)
    gaussianBlurSpinner.toolTipText =
        "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    val outputFileChooserPanel = OutputFileChooserPanel(prefs)
    container.add(outputFileChooserPanel)

    val okButton = JButton("Ok")
    container.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCTransductionPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val thresholdRadius = thresholdRadiusSpinner.value
        prefs.putRGCTransductionPref("thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value).toDouble()
        prefs.putRGCTransductionPref("gaussianBlur", gaussianBlurSigma)
        val targetChannel = targetChannelSpinner.value
        prefs.putRGCTransductionPref("morphologyChannel", targetChannel)
        val shouldRemoveAxonsFromTargetChannel = shouldRemoveAxonsFromTargetChannelCheckbox.isSelected
        prefs.putRGCTransductionPref("shouldRemoveAxonsFromTargetChannel", shouldRemoveAxonsFromTargetChannel)
        val transducedChannel = transducedChannelSpinner.value
        prefs.putRGCTransductionPref("transductionChannel", transducedChannel)
        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.field.text)
        prefs.putRGCTransductionPref("cellDiameter", cellDiameterChannelField.field.text as String)
        val shouldRemoveAxonsFromTransductionChannel = shouldRemoveAxonsFromTransductionChannelCheckbox.isSelected
        prefs.putRGCTransductionPref(
            "shouldRemoveAxonsFromTransductionChannel",
            shouldRemoveAxonsFromTransductionChannel
        )


        try {
            runRGCTransduction(
                inputDirectoryChooser.directory,
                shouldProcessFilesInNestedFolders,
                thresholdRadius,
                gaussianBlurSigma,
                targetChannel,
                shouldRemoveAxonsFromTargetChannel,
                transducedChannel,
                shouldRemoveAxonsFromTransductionChannel,
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
