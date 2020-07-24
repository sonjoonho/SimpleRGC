package simplergc.commands.batch.views.common

import simplergc.commands.batch.RGCBatch.OutputFormat
import java.awt.GridLayout
import java.io.File
import javax.swing.ButtonGroup
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.JRadioButton

const val COLUMN_WIDTH = 30

class OutputFileChooserPanel(initial: String, var format: String) : JPanel() {

    var file = File(initial)

    init {
        this.layout = GridLayout(0, 1)

        val resultsOutputPanel = JPanel()
        resultsOutputPanel.layout = GridLayout(0, 2)
        val resultsOutputLabel = ParameterLabel("Results output")
        resultsOutputPanel.add(resultsOutputLabel)
        val saveAsXLSXButton = JRadioButton("Save as a XLSX file (Recommended)")
        saveAsXLSXButton.isSelected = format == OutputFormat.XLSX
        addButtonActionListener(saveAsXLSXButton, OutputFormat.XLSX)
        val saveAsCSVButton = JRadioButton("Save as a CSV file(s)")
        saveAsCSVButton.isSelected = format == OutputFormat.CSV
        addButtonActionListener(saveAsCSVButton, OutputFormat.CSV)
        val bg = ButtonGroup()
        bg.add(saveAsXLSXButton)
        bg.add(saveAsCSVButton)
        val buttonPanel = JPanel()
        buttonPanel.layout = GridLayout(0, 1)
        buttonPanel.add(saveAsXLSXButton)
        buttonPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(buttonPanel)
        this.add(resultsOutputPanel)

        val outputFilePanel = JPanel()
        outputFilePanel.layout = GridLayout(0, 2)
        val label = ParameterLabel("Output file (if saving)")
        outputFilePanel.add(label)

        val chooserPanel = FileChooserPanel(file)

        outputFilePanel.add(chooserPanel)
        this.add(outputFilePanel)

        chooserPanel.browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.selectedFile
                chooserPanel.path.text = file.absolutePath
            }
        }
    }

    private fun addButtonActionListener(button: JRadioButton, value: String) {
        button.addActionListener {
            if (button.isSelected) {
                format = value
            }
        }
    }
}
