package simplergc.commands.batch.views.common

import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import simplergc.commands.batch.RGCBatch.OutputFormat

const val COLUMN_WIDTH = 30

class OutputFileChooserPanel(initial: String) : JPanel() {

    var file = File(initial)
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
        val fileName = FileChooserTextArea(file.absolutePath, 1, COLUMN_WIDTH)
        label.labelFor = browseButton
        fileChooserPanel.add(label)
        browseButtonPanel.add(fileName)
        browseButtonPanel.add(browseButton)
        fileChooserPanel.add(browseButtonPanel)
        this.add(fileChooserPanel)

        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val i = fileChooser.showOpenDialog(this)
            if (i == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.selectedFile
                fileName.text = file.absolutePath
            }
        }
    }
}
