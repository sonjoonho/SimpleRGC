package simplergc.commands.batch.views.common

import simplergc.commands.batch.RGCBatch
import simplergc.commands.batch.getRGCTransductionPref
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.putRGCTransductionPref
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import java.util.prefs.Preferences
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextArea

class OutputFileChooserPanel(container: JPanel, prefs: Preferences) : JPanel() {
    
    var file = File(prefs.getRGCTransductionPref("outputFile", ""))
    var format = RGCBatch.OutputFormat.CSV

    init {
        addMessage(container, "Output parameters")

        val resultsOutputPanel = JPanel()
        resultsOutputPanel.layout = GridLayout(0, 2)
        val resultsOutputLabel = JLabel("Results output")
        resultsOutputPanel.add(resultsOutputLabel)
        val saveAsCSVButton = JRadioButton("Save as a CSV file")
        val saveAsXMLButton = JRadioButton("Save as XML file")
        saveAsCSVButton.isSelected = format == RGCBatch.OutputFormat.CSV
        saveAsXMLButton.isSelected = !saveAsCSVButton.isSelected
        val bg = ButtonGroup()
        bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
        resultsOutputPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(JPanel())
        resultsOutputPanel.add(saveAsXMLButton)
        container.add(resultsOutputPanel)

        this.layout = GridLayout(0, 2)
        val label = JLabel("Output File (if saving)")
        val browseButtonPanel = JPanel()
        browseButtonPanel.layout = GridBagLayout()
        val browseButton = JButton("Browse")
        val fileName = JTextArea(1, 25)
        fileName.text = file.absolutePath
        label.labelFor = browseButton
        this.add(label)
        browseButtonPanel.add(fileName)
        browseButtonPanel.add(browseButton)
        this.add(browseButtonPanel)
        // TODO(sonjoonho): Very temporary I promise.
        prefs.putRGCCounterPref("saveAsCSV", saveAsCSVButton.isSelected)
        prefs.putRGCTransductionPref("saveAsCSV", saveAsCSVButton.isSelected)


        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val i = fileChooser.showOpenDialog(container)
            if (i == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.selectedFile
                fileName.text = file.absolutePath.takeLast(25)
                prefs.putRGCCounterPref("outputFile", file.absolutePath)
                prefs.putRGCTransductionPref("outputFile", file.absolutePath)
            }
        }
    }
}