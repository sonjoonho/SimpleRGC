package simplecolocalization.commands.batch.views

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
import simplecolocalization.commands.batch.RGCBatch
import simplecolocalization.commands.batch.controllers.runRGCTransduction
import simplecolocalization.commands.batch.getRGCTransductionPref
import simplecolocalization.commands.batch.putRGCTransductionPref
import simplecolocalization.commands.batch.views.common.addCellDiameterField
import simplecolocalization.commands.batch.views.common.addCheckBox
import simplecolocalization.commands.batch.views.common.addMessage
import simplecolocalization.commands.batch.views.common.addSpinner
import simplecolocalization.services.CellDiameterRange

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
    folderName.text = prefs.getRGCTransductionPref(prefs, "folderName", "").takeLast(25)
    inputFolderLabel.labelFor = button
    folderChooserPanel.add(inputFolderLabel)
    buttonPanel.add(folderName)
    buttonPanel.add(button)
    folderChooserPanel.add(buttonPanel)

    var inputFolder = if (prefs.getRGCTransductionPref(prefs, "folderName", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCTransductionPref(prefs, "folderName", ""))
    }
    button.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            inputFolder = fileChooser.selectedFile
            folderName.text = inputFolder!!.absolutePath.takeLast(25)
            prefs.putRGCTransductionPref(prefs, "folderName", inputFolder!!.absolutePath)
        }
    }

    panel.add(folderChooserPanel)

    val shouldProcessFilesInNestedFoldersCheckbox =
        addCheckBox(panel, "Batch process in nested sub-folders?", prefs, "shouldProcessFilesInNestedFolders", false)

    addMessage(panel, "<html><div align=\\\"left\\\">When performing batch colocalization, ensure that all input images have the same </br> channel ordering as specified below.</div></html>")

    val morphologyChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref(prefs, "morphologyChannel", 1), 1, 100, 1)
    val targetChannelSpinner = addSpinner(panel, "Cell morphology channel", morphologyChannelModel)

    val transductionChannelModel = SpinnerNumberModel(prefs.getRGCTransductionPref(prefs, "transductionChannel", 2), 1, 100, 1)
    val transducedChannelSpinner = addSpinner(panel, "Transduction channel", transductionChannelModel)

    addMessage(panel, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(panel, prefs, "cellDiameter", false)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCTransductionPref(prefs, "thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCTransductionPref(prefs, "gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)

    addMessage(panel, "Output parameters")

    val resultsOutputPanel = JPanel()
    resultsOutputPanel.layout = GridLayout(0, 2)
    val resultsOutputLabel = JLabel("Results output")
    resultsOutputPanel.add(resultsOutputLabel)
    val saveAsCSVButton = JRadioButton("Save as a CSV file")
    val saveAsXMLButton = JRadioButton("Save as XML file")
    saveAsCSVButton.isSelected = prefs.getRGCTransductionPref(prefs, "SaveAsCSV", true)
    saveAsXMLButton.isSelected = !prefs.getRGCTransductionPref(prefs, "SaveAsCSV", true)
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
    fileName.text = prefs.getRGCTransductionPref(prefs, "outputFile", "").takeLast(25)
    label.labelFor = button
    fileChooserPanel.add(label)
    browseButtonPanel.add(fileName)
    browseButtonPanel.add(browseButton)
    fileChooserPanel.add(browseButtonPanel)

    var outputFile = if (prefs.getRGCTransductionPref(prefs, "outputFile", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCTransductionPref(prefs, "outputFile", ""))
    }
    browseButton.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val i = fileChooser.showOpenDialog(panel)
        if (i == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.selectedFile
            fileName.text = outputFile!!.absolutePath.takeLast(25)
            prefs.putRGCTransductionPref(prefs, "outputFile", outputFile!!.absolutePath)
        }
    }

    panel.add(fileChooserPanel)

    val okButton = JButton("Ok")
    panel.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCTransductionPref(prefs, "shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        prefs.putRGCTransductionPref(prefs, "thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        prefs.putRGCTransductionPref(prefs, "gaussianBlur", gaussianBlurSigma)
        val outputFormat = when {
            saveAsCSVButton.isSelected -> RGCBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> RGCBatch.OutputFormat.XML
            else -> ""
        }
        val targetChannel = targetChannelSpinner.value as Int
        prefs.putRGCTransductionPref(prefs, "morphologyChannel", targetChannel)
        val transducedChannel = transducedChannelSpinner.value as Int
        prefs.putRGCTransductionPref(prefs, "transductionChannel", transducedChannel)

        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
        prefs.putRGCTransductionPref(prefs, "cellDiameter", cellDiameterChannelField.text)

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
