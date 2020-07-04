package simplergc.commands.batch.views

import javax.swing.BoxLayout
import javax.swing.SpinnerNumberModel
import simplergc.commands.batch.controllers.RGCController
import simplergc.commands.batch.models.RGCTransductionModel
import simplergc.commands.batch.views.common.CellDiameterField
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.OutputFileChooserPanel
import simplergc.commands.batch.views.common.RGCCheckbox
import simplergc.commands.batch.views.common.RGCSpinner
import simplergc.commands.batch.views.common.addMessage

class RGCTransductionView(model: RGCTransductionModel) : RGCView() {

    val inputDirectoryChooser = InputDirectoryChooserPanel(this, model.inputDirectory)

    val shouldProcessFilesInNestedFoldersCheckbox =
        RGCCheckbox("Batch process in nested sub-folders?", model.shouldProcessFilesInNestedFolders)

    private val targetChannelModel = SpinnerNumberModel(model.targetChannel, 1, 100, 1)
    val targetChannelSpinner = RGCSpinner("Cell morphology channel", targetChannelModel)

    val shouldRemoveAxonsFromTargetChannelCheckbox =
        RGCCheckbox("Exclude Axons from target channel", model.shouldRemoveAxonsFromTargetChannel)

    private val transductionChannelModel = SpinnerNumberModel(model.transductionChannel, 1, 100, 1)
    val transductionChannelSpinner = RGCSpinner("Transduction channel", transductionChannelModel)

    val shouldRemoveAxonsFromTransductionChannelCheckbox =
        RGCCheckbox("Exclude Axons from Transduction channel", model.shouldRemoveAxonsFromTransductionChannel)

    val cellDiameterChannelField = CellDiameterField(model.cellDiameter)

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

        this.add(targetChannelSpinner)
        targetChannelSpinner.toolTipText = "Used as minimum/maximum diameter when identifying cells"

        this.add(shouldRemoveAxonsFromTargetChannelCheckbox)

        this.add(transductionChannelSpinner)

        this.add(shouldRemoveAxonsFromTransductionChannelCheckbox)

        addMessage(this, "Image processing parameters")

        this.add(cellDiameterChannelField)

        this.add(thresholdRadiusSpinner)
        thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

        this.add(gaussianBlurSpinner)
        gaussianBlurSpinner.toolTipText =
            "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

        this.add(outputFileChooserPanel)

        this.add(okButton)
    }

    override fun addListeners(controller: RGCController) {
        okButton.addActionListener(controller.okButton())
    }
}
