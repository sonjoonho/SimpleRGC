package simplergc.commands.batch.views

import simplergc.commands.batch.controllers.RGCController
import simplergc.commands.batch.models.RGCTransductionModel
import simplergc.commands.batch.views.common.CellDiameterField
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.RGCCheckbox
import simplergc.commands.batch.views.common.RGCSpinner
import simplergc.commands.batch.views.common.addLabel
import simplergc.commands.batch.views.common.addMessage
import javax.swing.BoxLayout
import javax.swing.SpinnerNumberModel

class RGCTransductionView(model: RGCTransductionModel) : RGCView() {

    val inputDirectoryChooser = InputDirectoryChooserPanel(this, model.inputDirectory)

    val shouldProcessFilesInNestedFoldersCheckbox =
        RGCCheckbox("Batch process in nested sub-folders?", model.shouldProcessFilesInNestedFolders)

    private val targetChannelModel = SpinnerNumberModel(model.targetChannel, 1, 100, 1)
    val targetChannelSpinner = RGCSpinner("Morphology channel", targetChannelModel)

    val shouldRemoveAxonsFromTargetChannelCheckbox =
        RGCCheckbox("Exclude axons", model.shouldRemoveAxonsFromTargetChannel)

    private val transductionChannelModel = SpinnerNumberModel(model.transductionChannel, 1, 100, 1)
    val transductionChannelSpinner = RGCSpinner("Transduction channel", transductionChannelModel)

    val shouldRemoveAxonsFromTransductionChannelCheckbox =
        RGCCheckbox("Exclude axons", model.shouldRemoveAxonsFromTransductionChannel)

    val cellDiameterField = CellDiameterField(model.cellDiameter)

    val thresholdRadiusModel = SpinnerNumberModel(model.thresholdRadius, 1, 1000, 1)
    val thresholdRadiusSpinner = RGCSpinner("Local threshold radius", thresholdRadiusModel)

    val gaussianBlurModel = SpinnerNumberModel(model.gaussianBlur.toInt(), 1, 50, 1)
    val gaussianBlurSpinner = RGCSpinner("Gaussian blur sigma", gaussianBlurModel)

    val outputFileChooserPanel = OutputFileChooserPanel(model.outputFile, model.outputFormat)

    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)

        this.add(inputDirectoryChooser)

        this.add(shouldProcessFilesInNestedFoldersCheckbox)

        addMessage(
            this,
            "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>"
        )

        addLabel(this, "Morphology cells")

        this.add(targetChannelSpinner)

        this.add(cellDiameterField)

        this.add(shouldRemoveAxonsFromTargetChannelCheckbox)

        addLabel(this, "Transduction cells")

        this.add(transductionChannelSpinner)

        this.add(shouldRemoveAxonsFromTransductionChannelCheckbox)

        addLabel(this, "Image processing parameters")

        this.add(thresholdRadiusSpinner)
        thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

        this.add(gaussianBlurSpinner)
        gaussianBlurSpinner.toolTipText =
            "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

        addLabel(this, "Output parameters")

        this.add(outputFileChooserPanel)

        this.add(okButton)
    }

    override fun addListeners(controller: RGCController) {
        okButton.addActionListener(controller.okButton())
    }
}
