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
import simplecolocalization.commands.batch.controllers.runSimpleCellCounter
import simplecolocalization.commands.batch.views.common.addCheckBox
import simplecolocalization.commands.batch.views.common.addFileChooser
import simplecolocalization.commands.batch.views.common.addSpinner
import simplecolocalization.widgets.AlignedTextWidget

/** Creates the Simple Cell Counter GUI. */
fun simpleCellCounterPanel(context: Context): JPanel {
    // TODO: Make this pretty
    // TODO: Make User parameters persist
    val panel = JPanel()
    panel.layout = GridLayout(0, 1)

    var inputFolder: File? = null
    val button =
        addFileChooser(panel, "Input folder")
    button.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            inputFolder = fileChooser.selectedFile
        }
    }

    val shouldProcessFilesInNestedFoldersCheckbox = addCheckBox(panel, "Batch process in nested sub-folders?")

    val channelModel = SpinnerNumberModel(1, 0, 10, 1)
    val channelSpinner = addSpinner(panel, "Select channel to use", channelModel)

    val imageProcessingParametersLabel = JLabel("Image processing parameters")
    panel.add(imageProcessingParametersLabel)

    // TODO: Hook this up so it actually works
    val cellDiameterLabel = JLabel("Cell diameter(px)")
    val cellDiameterWidget = AlignedTextWidget()
    panel.add(cellDiameterLabel)

    val thresholdRadiusModel = SpinnerNumberModel(20, 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

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
    val browseButton = addFileChooser(
        panel,
        "Output File (if saving)"
    )
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
        val channel = channelSpinner.value as Int
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        val outputFormat = when {
            saveAsCSVButton.isSelected -> SimpleBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> SimpleBatch.OutputFormat.XML
            else -> ""
        }
        try {
            runSimpleCellCounter(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                channel,
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                outputFile,
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
    return panel
}
