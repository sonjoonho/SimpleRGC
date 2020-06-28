package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.io.FileNotFoundException
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SpinnerNumberModel
import org.scijava.Context
import simplergc.commands.batch.controllers.runRGCTransduction
import simplergc.commands.batch.models.RGCTransductionModel
import simplergc.commands.batch.views.common.CellDiameterField
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.RGCCheckbox
import simplergc.commands.batch.views.common.RGCSpinner
import simplergc.commands.batch.views.common.addMessage
import simplergc.services.CellDiameterRange

/** Creates the Simple Colocalizer GUI. */
fun rgcTransductionPanel(context: Context, model: RGCTransductionModel): JPanel {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val inputDirectoryChooser = InputDirectoryChooserPanel(container, model.inputDirectory)
    container.add(inputDirectoryChooser)

    val shouldProcessFilesInNestedFoldersCheckbox =
        RGCCheckbox("Batch process in nested sub-folders?", model.shouldProcessFilesInNestedFolders)
    container.add(shouldProcessFilesInNestedFoldersCheckbox)

    addMessage(
        container,
        "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>"
    )

    val targetChannelModel = SpinnerNumberModel(model.targetChannel, 1, 100, 1)
    val targetChannelSpinner = RGCSpinner("Cell morphology channel", targetChannelModel)
    container.add(targetChannelSpinner)
    targetChannelSpinner.toolTipText = "Used as minimum/maximum diameter when identifying cells"
    val shouldRemoveAxonsFromTargetChannelCheckbox =
        RGCCheckbox("Exclude Axons from target channel", model.shouldRemoveAxonsFromTargetChannel)
    container.add(shouldRemoveAxonsFromTargetChannelCheckbox)

    val transductionChannelModel = SpinnerNumberModel(model.transductionChannel, 1, 100, 1)
    val transducedChannelSpinner = RGCSpinner("Transduction channel", transductionChannelModel)
    container.add(transducedChannelSpinner)
    val shouldRemoveAxonsFromTransductionChannelCheckbox = RGCCheckbox("Exclude Axons from Transduction channel", model.shouldRemoveAxonsFromTransductionChannel)
    container.add(shouldRemoveAxonsFromTransductionChannelCheckbox)

    addMessage(container, "Image processing parameters")

    val cellDiameterChannelField = CellDiameterField(model.cellDiameter)
    container.add(cellDiameterChannelField)

    val thresholdRadiusModel = SpinnerNumberModel(model.thresholdRadius, 1, 1000, 1)
    val thresholdRadiusSpinner = RGCSpinner("Local threshold radius", thresholdRadiusModel)
    container.add(thresholdRadiusSpinner)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(model.gaussianBlur.toInt(), 1, 50, 1)
    val gaussianBlurSpinner = RGCSpinner("Gaussian blur sigma", gaussianBlurModel)
    container.add(gaussianBlurSpinner)
    gaussianBlurSpinner.toolTipText =
        "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    val outputFileChooserPanel = OutputFileChooserPanel(model.outputFile)
    container.add(outputFileChooserPanel)

    val okButton = JButton("Ok")
    container.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        model.shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFolders
        val thresholdRadius = thresholdRadiusSpinner.value
        model.thresholdRadius = thresholdRadius
        val gaussianBlurSigma = (gaussianBlurSpinner.value).toDouble()
        model.gaussianBlur = gaussianBlurSigma
        val targetChannel = targetChannelSpinner.value
        model.targetChannel = targetChannel
        val shouldRemoveAxonsFromTargetChannel = shouldRemoveAxonsFromTargetChannelCheckbox.isSelected
        model.shouldRemoveAxonsFromTargetChannel = shouldRemoveAxonsFromTargetChannel
        val transducedChannel = transducedChannelSpinner.value
        model.transductionChannel = transducedChannel
        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.field.text)
        model.cellDiameter = cellDiameterChannelField.field.text
        val shouldRemoveAxonsFromTransductionChannel = shouldRemoveAxonsFromTransductionChannelCheckbox.isSelected
        model.shouldRemoveAxonsFromTransductionChannel = shouldRemoveAxonsFromTransductionChannel

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
