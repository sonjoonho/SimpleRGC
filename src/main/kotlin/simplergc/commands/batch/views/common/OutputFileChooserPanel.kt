package simplergc.commands.batch.views.common

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
import simplergc.commands.batch.getRGCTransductionPref
import simplergc.commands.batch.putRGCCounterPref
import simplergc.commands.batch.putRGCTransductionPref
import simplergc.services.OutputFormat

const val COLUMN_WIDTH = 25

class OutputFileChooserPanel(prefs: Preferences) : JPanel() {

    var file = File(prefs.getRGCTransductionPref("outputFile", ""))
    var format = OutputFormat.CSV

    init {
        this.layout = GridLayout(0, 1)

        val resultsOutputPanel = JPanel()
        resultsOutputPanel.layout = GridLayout(0, 2)
        val resultsOutputLabel = JLabel("Results output")
        resultsOutputPanel.add(resultsOutputLabel)
        val saveAsCSVButton = JRadioButton("Save as a CSV file")
        val saveAsXMLButton = JRadioButton("Save as XML file")
        saveAsCSVButton.isSelected = format == OutputFormat.CSV
        saveAsXMLButton.isSelected = !saveAsCSVButton.isSelected
        val bg = ButtonGroup()
        bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
        resultsOutputPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(JPanel())
        resultsOutputPanel.add(saveAsXMLButton)
        this.add(resultsOutputPanel)

        val fileChooserPanel = JPanel()
        fileChooserPanel.layout = GridLayout(0, 2)
        val label = JLabel("Output File (if saving)")
        val browseButtonPanel = JPanel()
        browseButtonPanel.layout = GridBagLayout()
        val browseButton = JButton("Browse")
        val fileName = JTextArea(1, COLUMN_WIDTH)
        fileName.text = file.absolutePath
        label.labelFor = browseButton
        fileChooserPanel.add(label)
        browseButtonPanel.add(fileName)
        browseButtonPanel.add(browseButton)
        fileChooserPanel.add(browseButtonPanel)
        this.add(fileChooserPanel)
        // TODO(sonjoonho): Very temporary I promise.
        prefs.putRGCCounterPref("saveAsCSV", saveAsCSVButton.isSelected)
        prefs.putRGCTransductionPref("saveAsCSV", saveAsCSVButton.isSelected)

        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val i = fileChooser.showOpenDialog(this)
            if (i == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.selectedFile
                fileName.text = file.absolutePath.takeLast(COLUMN_WIDTH)
                prefs.putRGCCounterPref("outputFile", file.absolutePath)
                prefs.putRGCTransductionPref("outputFile", file.absolutePath)
            }
        }
    }
}
