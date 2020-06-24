package simplecolocalization.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import java.io.FileNotFoundException
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextArea
import javax.swing.SpinnerNumberModel
import org.scijava.Context
import simplecolocalization.commands.batch.RGCBatch
import simplecolocalization.commands.batch.controllers.runRGCCounter
import simplecolocalization.commands.batch.views.common.addCellDiameterField
import simplecolocalization.commands.batch.views.common.addCheckBox
import simplecolocalization.commands.batch.views.common.addMessage
import simplecolocalization.commands.batch.views.common.addSpinner
import simplecolocalization.services.CellDiameterRange

/** Creates the Simple Cell Counter GUI. */
fun rgcCounterPanel(context: Context): JPanel {
    // TODO: Make User parameters persist
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

    // TODO: Refactor file choosing to reduce duplication
    val folderChooserPanel = JPanel()
    folderChooserPanel.layout = GridLayout(0, 2)
    val inputFolderLabel = JLabel("Input folder")
    val buttonPanel = JPanel()
    buttonPanel.layout = GridBagLayout()
    val button = JButton("Browse")
    val folderName = JTextArea(1, 25)
    inputFolderLabel.labelFor = button
    folderChooserPanel.add(inputFolderLabel)
    buttonPanel.add(folderName)
    buttonPanel.add(button)
    folderChooserPanel.add(buttonPanel)

    var inputFolder: File? = null
    button.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            inputFolder = fileChooser.selectedFile
            folderName.append(inputFolder!!.absolutePath.takeLast(25))
        }
    }

    panel.add(folderChooserPanel)

    val shouldProcessFilesInNestedFoldersCheckbox = addCheckBox(panel, "Batch process in nested sub-folders?")

    val channelModel = SpinnerNumberModel(1, 0, 10, 1)
    val channelSpinner = addSpinner(panel, "Select channel to use", channelModel)

    addMessage(panel, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(panel)

    val thresholdRadiusModel = SpinnerNumberModel(20, 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

    val gaussianBlurModel = SpinnerNumberModel(3, 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)

    addMessage(panel, "Output parameters")

    val resultsOutputPanel = JPanel()
    resultsOutputPanel.layout = GridLayout(0, 2)
    val resultsOutputLabel = JLabel("Results output")
    resultsOutputPanel.add(resultsOutputLabel)
    val saveAsCSVButton = JRadioButton("Save as a CSV file")
    val saveAsXMLButton = JRadioButton("Save as XML file")
    val bg = ButtonGroup()
    bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
    resultsOutputPanel.add(saveAsCSVButton)
    resultsOutputPanel.add(JPanel())
    resultsOutputPanel.add(saveAsXMLButton)
    panel.add(resultsOutputPanel)

    // TODO: Refactor file choosing to reduce duplication
    val fileChooserPanel = JPanel()
    fileChooserPanel.layout = GridLayout(0, 2)
    val label = JLabel("Output File (if saving)")
    val browseButtonPanel = JPanel()
    browseButtonPanel.layout = GridBagLayout()
    val browseButton = JButton("Browse")
    val fileName = JTextArea(1, 25)
    label.labelFor = button
    fileChooserPanel.add(label)
    browseButtonPanel.add(fileName)
    browseButtonPanel.add(browseButton)
    fileChooserPanel.add(browseButtonPanel)

    var outputFile: File? = null
    browseButton.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.selectedFile
            fileName.append(outputFile!!.absolutePath.takeLast(25))
        }
    }

    panel.add(fileChooserPanel)

    val okButton = JButton("Ok")
    panel.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        val channel = channelSpinner.value as Int
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        val outputFormat = when {
            saveAsCSVButton.isSelected -> RGCBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> RGCBatch.OutputFormat.XML
            else -> ""
        }

        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)

        try {
            runRGCCounter(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                channel,
                thresholdRadius,
                gaussianBlurSigma,
                cellDiameterRange,
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

    panel.add(JPanel())
    return panel
}
