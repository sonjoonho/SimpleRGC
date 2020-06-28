package simplergc.commands.batch.views.common

import simplergc.commands.batch.getRGCTransductionPref
import simplergc.commands.batch.putRGCTransductionPref
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class InputDirectoryChooserPanel(container: JPanel, prefs: Preferences) : JPanel() {

    var inputFolder = if (prefs.getRGCTransductionPref("folderName", "").isEmpty()) {
        null
    } else {
        File(prefs.getRGCTransductionPref("folderName", ""))
    }

    init {
        this.layout = GridLayout(0, 2)
        val inputFolderLabel = JLabel("Input folder")
        val buttonPanel = JPanel()
        buttonPanel.layout = GridBagLayout()
        val browseButton = JButton("Browse")
        val folderName = JTextArea(1, 25)
        folderName.text = prefs.getRGCTransductionPref("folderName", "").takeLast(25)
        inputFolderLabel.labelFor = browseButton
        this.add(inputFolderLabel)
        buttonPanel.add(folderName)
        buttonPanel.add(browseButton)
        this.add(buttonPanel)


        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            val i = fileChooser.showOpenDialog(container)
            if (i == JFileChooser.APPROVE_OPTION) {
                inputFolder = fileChooser.selectedFile
                folderName.text = inputFolder!!.absolutePath.takeLast(25)
                prefs.putRGCTransductionPref("folderName", inputFolder!!.absolutePath)
            }
        }
    }
}