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

class OutputFileChooserPanel(initial: String, var format: String) : JPanel() {

    var file = File(initial)

    init {
        this.layout = GridLayout(0, 1)

        val resultsOutputPanel = JPanel()
        resultsOutputPanel.layout = GridLayout(0, 2)
        val resultsOutputLabel = JLabel("Results output")
        resultsOutputPanel.add(resultsOutputLabel)
        val saveAsXLSXButton = JRadioButton("Save as Excel file (Recommended)")
        saveAsXLSXButton.isSelected = format == OutputFormat.XLSX
        val saveAsCSVButton = JRadioButton("Save as a CSV file(s)")
        saveAsCSVButton.isSelected = format == OutputFormat.CSV
        val bg = ButtonGroup()
        bg.add(saveAsXLSXButton)
        resultsOutputPanel.add(saveAsXLSXButton)
        bg.add(saveAsCSVButton)
        resultsOutputPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(JPanel())
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
