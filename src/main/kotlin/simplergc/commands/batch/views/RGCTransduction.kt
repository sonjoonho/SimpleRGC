package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import java.io.FileNotFoundException
import java.util.prefs.Preferences
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
import simplergc.commands.batch.RGCBatch
import simplergc.commands.batch.controllers.runRGCTransduction
import simplergc.commands.batch.getRGCTransductionPref
import simplergc.commands.batch.putRGCTransductionPref
import simplergc.commands.batch.views.common.addCellDiameterField
import simplergc.commands.batch.views.common.addCheckBox
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.addSpinner
import simplergc.services.CellDiameterRange

/** Creates the Simple Colocalizer GUI. */
fun rgcTransductionPanel(context: Context, prefs: Preferences): JPanel {
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
    folderName.text = prefs.getRGCTransductionPref("folderName", "").takeLast(25)
    inputFolderLabel.labelFor = button
    folderChooserPanel.add(inputFolderLabel)
    buttonPanel.add(folderName)
    buttonPanel.add(button)
    folderChooserPanel.add(buttonPanel)

    var inputFolder = if (prefs.getRGCTransductionPref("folderName", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCTransductionPref("folderName", ""))
    }
    button.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            inputFolder = fileChooser.selectedFile
            folderName.text = inputFolder!!.absolutePath.takeLast(25)
            prefs.putRGCTransductionPref("folderName", inputFolder!!.absolutePath)
        }
    }

    panel.add(folderChooserPanel)

    val shouldProcessFilesInNestedFoldersCheckbox =
        addCheckBox(panel, "Batch process in nested sub-folders?", prefs, "shouldProcessFilesInNestedFolders", false)

    addMessage(panel, "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>")

    val morphologyChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("morphologyChannel", 1), 1, 100, 1)
    val targetChannelSpinner = addSpinner(panel, "Cell morphology channel", morphologyChannelModel)
    targetChannelSpinner.toolTipText = "Used as minimum/maximum diameter when identifying cells"

    val transductionChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref("transductionChannel", 2), 1, 100, 1)
    val transducedChannelSpinner = addSpinner(panel, "Transduction channel", transductionChannelModel)

    addMessage(panel, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(panel, prefs, "cellDiameter", false)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCTransductionPref("thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCTransductionPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)
    gaussianBlurSpinner.toolTipText = "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    addMessage(panel, "Output parameters")

    val resultsOutputPanel = JPanel()
    resultsOutputPanel.layout = GridLayout(0, 2)
    val resultsOutputLabel = JLabel("Results output")
    resultsOutputPanel.add(resultsOutputLabel)
    val saveAsCSVButton = JRadioButton("Save as a CSV file")
    val saveAsXMLButton = JRadioButton("Save as XML file")
    saveAsCSVButton.isSelected = prefs.getRGCTransductionPref("SaveAsCSV", true)
    saveAsXMLButton.isSelected = !prefs.getRGCTransductionPref("SaveAsCSV", true)
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
    fileName.text = prefs.getRGCTransductionPref("outputFile", "").takeLast(25)
    label.labelFor = button
    fileChooserPanel.add(label)
    browseButtonPanel.add(fileName)
    browseButtonPanel.add(browseButton)
    fileChooserPanel.add(browseButtonPanel)

    var outputFile = if (prefs.getRGCTransductionPref("outputFile", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCTransductionPref("outputFile", ""))
    }
    browseButton.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.selectedFile
            fileName.text = outputFile!!.absolutePath.takeLast(25)
            prefs.putRGCTransductionPref("outputFile", outputFile!!.absolutePath)
        }
    }

    panel.add(fileChooserPanel)

    val okButton = JButton("Ok")
    panel.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCTransductionPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        prefs.putRGCTransductionPref("thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        prefs.putRGCTransductionPref("gaussianBlur", gaussianBlurSigma)
        val outputFormat = when {
            saveAsCSVButton.isSelected -> RGCBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> RGCBatch.OutputFormat.XML
            else -> ""
        }
        val targetChannel = targetChannelSpinner.value as Int
        prefs.putRGCTransductionPref("morphologyChannel", targetChannel)
        val transducedChannel = transducedChannelSpinner.value as Int
        prefs.putRGCTransductionPref("transductionChannel", transducedChannel)

        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
        prefs.putRGCTransductionPref("cellDiameter", cellDiameterChannelField.text)

        try {
            runRGCTransduction(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                targetChannel,
                transducedChannel,
                cellDiameterRange,
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
