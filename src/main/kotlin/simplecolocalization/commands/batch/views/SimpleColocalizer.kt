package simplecolocalization.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.awt.GridLayout
import java.io.File
import java.io.FileNotFoundException
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SpinnerNumberModel
import org.scijava.Context
import simplecolocalization.commands.batch.SimpleBatch
import simplecolocalization.commands.batch.controllers.runSimpleColocalizer
import simplecolocalization.commands.batch.views.common.addCheckBox
import simplecolocalization.commands.batch.views.common.addFileChooser
import simplecolocalization.commands.batch.views.common.addSpinner
import simplecolocalization.widgets.AlignedTextWidget

/** Creates the Simple Colocalizer GUI. */
fun simpleColocalizerPanel(context: Context): JPanel {
    // TODO: Make this pretty
    val panel = JPanel()
    panel.layout = GridLayout(0, 1)

    var inputFolder: File? = null
    val button = addFileChooser(panel, "Input folder")
    button.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            inputFolder = fileChooser.selectedFile
        }
    }

    val shouldProcessFilesInNestedFoldersCheckbox = addCheckBox(panel, "Batch process in nested sub-folders?")

    val colocalizationInstructionLabel =
        JLabel("<html><div align=\"right\">\nWhen performing batch colocalization, ensure that <br />all input images have the same channel ordering as<br />specified below.</div></html>")
    panel.add(colocalizationInstructionLabel)

    val channel1Model = SpinnerNumberModel(1, 1, 100, 1)
    val targetChannelSpinner = addSpinner(panel, "Cell morphology channel 1", channel1Model)

    val channel2Model = SpinnerNumberModel(0, 0, 100, 1)
    val allCellsChannelSpinner = addSpinner(panel, "Cell morphology channel 2 (0 to disable)", channel2Model)

    val transductionChannel1Model = SpinnerNumberModel(2, 1, 100, 1)
    val transducedChannelSpinner = addSpinner(panel, "Transduction channel", transductionChannel1Model)

    val imageProcessingParametersLabel = JLabel("Image processing parameters")
    panel.add(imageProcessingParametersLabel)

    // TODO: Hook this up so it actually works
    val cellDiameterChannel1Label = JLabel("Cell diameter(px)")
    val cellDiameterChanne1Widget = AlignedTextWidget()
    panel.add(cellDiameterChannel1Label)

    val thresholdRadiusModel = SpinnerNumberModel(20, 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

    // TODO: Hook this up so it actually works
    val cellDiameterChannel2Label = JLabel("Cell diameter(px)")
    val cellDiameterChannel2Widget = AlignedTextWidget()
    panel.add(cellDiameterChannel2Label)

    val gaussianBlurModel = SpinnerNumberModel(3, 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)

    val outputParamsLabel = JLabel("Output parameters")
    panel.add(outputParamsLabel)

    val resultsOutputPanel = JPanel()
    val resultsOutputLabel = JLabel("Results output")
    val saveAsCSVButton = JRadioButton("Save as a CSV file")
    val saveAsXMLButton = JRadioButton("Save as XML file")
    val bg = ButtonGroup()
    bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
    resultsOutputPanel.add(resultsOutputLabel)
    resultsOutputPanel.add(saveAsCSVButton)
    resultsOutputPanel.add(saveAsXMLButton)
    panel.add(resultsOutputPanel)

    var outputFile: File? = null
    val browseButton = addFileChooser(panel, "Output File (if saving)")
    browseButton.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.selectedFile
        }
    }

    val okButton = JButton("Ok")
    panel.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        val outputFormat = when {
            saveAsCSVButton.isSelected -> SimpleBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> SimpleBatch.OutputFormat.XML
            else -> ""
        }
        val targetChannel = targetChannelSpinner.value as Int
        val transducedChannel = transducedChannelSpinner.value as Int
        val allCellsChannel = allCellsChannelSpinner.value as Int

        try {
            runSimpleColocalizer(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                targetChannel,
                transducedChannel,
                allCellsChannel,
                outputFile,
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
    return panel
}
