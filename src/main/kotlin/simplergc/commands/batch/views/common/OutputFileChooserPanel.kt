package simplergc.commands.batch.views.common

import java.awt.GridLayout
import java.io.File
import javax.swing.ButtonGroup
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
        val saveAsCSVButton = JRadioButton("Save as a CSV file")
        saveAsCSVButton.isSelected = format == OutputFormat.CSV
        val bg = ButtonGroup()
        bg.add(saveAsCSVButton)
        resultsOutputPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(JPanel())
        this.add(resultsOutputPanel)

        val outputFilePanel = JPanel()
        outputFilePanel.layout = GridLayout(0, 2)
        val label = JLabel("Output File (if saving)")
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
}
