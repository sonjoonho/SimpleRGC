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
import simplergc.commands.batch.controllers.runRGCCounter
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.views.common.InputDirectoryChooserPanel
import simplergc.commands.batch.views.common.addCellDiameterField
import simplergc.commands.batch.views.common.addCheckBox
import simplergc.commands.batch.views.common.addMessage
import simplergc.commands.batch.views.common.addSpinner
import simplergc.services.CellDiameterRange

/** Creates the RGC Counter GUI. */
fun rgcCounterPanel(context: Context, prefs: Preferences): JPanel {
    val container = JPanel()
    container.layout = BoxLayout(container, BoxLayout.Y_AXIS)

    val inputDirectoryChooser = InputDirectoryChooserPanel(container, prefs)
    container.add(inputDirectoryChooser)

    val shouldProcessFilesInNestedFoldersCheckbox =
        addCheckBox(container, "Batch process in nested sub-folders?", prefs, "shouldProcessFilesInNestedFolders", true)

    val channelModel = SpinnerNumberModel(prefs.getRGCCounterPref("channelToUse", 1), 0, 10, 1)
    val channelSpinner = addSpinner(container, "Select channel to use", channelModel)

    addMessage(container, "Image processing parameters")

    val cellDiameterChannelField = addCellDiameterField(container, prefs, "cellDiameter", true)

    val thresholdRadiusModel = SpinnerNumberModel(prefs.getRGCCounterPref("thresholdRadius", 20), 1, 1000, 1)
    val thresholdRadiusSpinner = addSpinner(container, "Local threshold radius", thresholdRadiusModel)
    thresholdRadiusSpinner.toolTipText = "The radius of the local domain over which the threshold will be computed."

    val gaussianBlurModel = SpinnerNumberModel(prefs.getRGCCounterPref("gaussianBlur", 3.0).toInt(), 1, 50, 1)
    val gaussianBlurSpinner = addSpinner(container, "Gaussian blur sigma", gaussianBlurModel)
    gaussianBlurSpinner.toolTipText = "Sigma value used for blurring the image during the processing, a lower value is recommended if there are lots of cells densely packed together"

    val shouldRemoveAxonsCheckbox = addCheckBox(container, "Remove Axons", prefs, "shouldRemoveAxons", true)

    addMessage(container, "Output parameters")

    val resultsOutputPanel = JPanel()
    resultsOutputPanel.layout = GridLayout(0, 2)
    val resultsOutputLabel = JLabel("Results output")
    resultsOutputPanel.add(resultsOutputLabel)
    val saveAsCSVButton = JRadioButton("Save as a CSV file")
    val saveAsXMLButton = JRadioButton("Save as XML file")
    saveAsCSVButton.isSelected = prefs.getRGCCounterPref("SaveAsCSV", true)
    saveAsXMLButton.isSelected = !prefs.getRGCCounterPref("SaveAsCSV", true)
    val bg = ButtonGroup()
    bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
    resultsOutputPanel.add(saveAsCSVButton)
    resultsOutputPanel.add(JPanel())
    resultsOutputPanel.add(saveAsXMLButton)
    container.add(resultsOutputPanel)

    // TODO: Refactor file choosing to reduce duplication
    val fileChooserPanel = JPanel()
    fileChooserPanel.layout = GridLayout(0, 2)
    val label = JLabel("Output File (if saving)")
    val browseButtonPanel = JPanel()
    browseButtonPanel.layout = GridBagLayout()
    val browseButton = JButton("Browse")
    val fileName = JTextArea(1, 25)
    fileName.text = prefs.getRGCCounterPref("outputFile", "").takeLast(25)
    label.labelFor = browseButton
    fileChooserPanel.add(label)
    browseButtonPanel.add(fileName)
    browseButtonPanel.add(browseButton)
    fileChooserPanel.add(browseButtonPanel)

    var outputFile = if (prefs.getRGCCounterPref("outputFile", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCCounterPref("outputFile", ""))
    }
    browseButton.addActionListener {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val i = fileChooser.showOpenDialog(container)
        if (i == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.selectedFile
            fileName.text = outputFile!!.absolutePath.takeLast(25)
            prefs.putRGCCounterPref("outputFile", outputFile!!.absolutePath)
        }
    }

    container.add(fileChooserPanel)

    val okButton = JButton("Ok")
    container.add(okButton)
    okButton.addActionListener {
        val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
        prefs.putRGCCounterPref("shouldProcessFilesInNestedFolders", shouldProcessFilesInNestedFolders)
        val channel = channelSpinner.value as Int
        prefs.putRGCCounterPref("channelToUse", channel)
        val thresholdRadius = thresholdRadiusSpinner.value as Int
        prefs.putRGCCounterPref("thresholdRadius", thresholdRadius)
        val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
        prefs.putRGCCounterPref("gaussianBlur", gaussianBlurSigma)
        val shouldRemoveAxons = shouldRemoveAxonsCheckbox.isSelected
        prefs.putRGCCounterPref("shouldRemoveAxons", shouldRemoveAxons)
        val outputFormat = when {
            saveAsCSVButton.isSelected -> RGCBatch.OutputFormat.CSV
            saveAsXMLButton.isSelected -> RGCBatch.OutputFormat.XML
            else -> ""
        }
        prefs.putRGCCounterPref("saveAsCSV", saveAsCSVButton.isSelected)

        val cellDiameterRange = CellDiameterRange.parseFromText(cellDiameterChannelField.text)
        prefs.putRGCCounterPref("cellDiameter", cellDiameterChannelField.text)

        try {
            runRGCCounter(
                inputDirectoryChooser.inputFolder,
                shouldProcessFilesInNestedFolders,
                channel,
                thresholdRadius,
                gaussianBlurSigma,
                shouldRemoveAxons,
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

    container.add(JPanel())
    return container
}
