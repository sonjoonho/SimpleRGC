package simplergc.commands.batch.views

import simplergc.commands.batch.controllers.RGCController
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.views.common.CellDiameterField
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.RGCCheckbox
import simplergc.commands.batch.views.common.RGCSpinner
import simplergc.commands.batch.views.common.addLabel
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.SpinnerNumberModel

class RGCCounterView(frame: JFrame, model: RGCCounterModel) : RGCView(frame) {

    val inputDirectoryChooser = InputDirectoryChooserPanel(this, model.inputDirectory)

    val shouldProcessFilesInNestedFoldersCheckbox =
        RGCCheckbox(
            "Batch process in nested sub-folders?",
            model.shouldProcessFilesInNestedFolders
        )

    private val channelModel = SpinnerNumberModel(model.channelToUse, 0, 10, 1)
    val channelSpinner = RGCSpinner("Select channel to use", channelModel)

    val cellDiameterChannelField = CellDiameterField(model.cellDiameter)

    private val thresholdRadiusModel = SpinnerNumberModel(model.thresholdRadius, 1, 1000, 1)
    val thresholdRadiusSpinner = RGCSpinner("Local threshold radius", thresholdRadiusModel)

    private val gaussianBlurModel = SpinnerNumberModel(model.gaussianBlur.toInt(), 1, 50, 1)
    val gaussianBlurSpinner = RGCSpinner("Gaussian blur sigma", gaussianBlurModel)

    val shouldRemoveAxonsCheckbox = RGCCheckbox("Remove Axons", model.shouldRemoveAxons)

    val outputFileChooserPanel = OutputFileChooserPanel(model.outputFile, model.outputFormat)

    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)

        this.add(inputDirectoryChooser)

        this.add(shouldProcessFilesInNestedFoldersCheckbox)

        this.add(channelSpinner)

        addLabel(this, "Image processing parameters")
        this.add(cellDiameterChannelField)

        this.add(thresholdRadiusSpinner)
        thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

        this.add(gaussianBlurSpinner)
        gaussianBlurSpinner.toolTipText =
            "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

        this.add(shouldRemoveAxonsCheckbox)

        addLabel(this, "Output parameters")
        this.add(outputFileChooserPanel)

        this.add(okButton)
    }

    override fun addListeners(controller: RGCController) {
        okButton.addActionListener(controller.okButton())
    }
}
