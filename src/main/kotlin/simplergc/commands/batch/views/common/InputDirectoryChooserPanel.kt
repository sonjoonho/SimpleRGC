package simplergc.commands.batch.views.common

import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

class InputDirectoryChooserPanel(container: JPanel, initial: String) : JPanel() {

    var directory = File(initial)

    init {
        this.layout = GridLayout(0, 2)
        val inputFolderLabel = JLabel("Input folder")
        val buttonPanel = JPanel()
        buttonPanel.layout = GridBagLayout()
        val browseButton = JButton("Browse")
        val folderName = FileChooserTextArea(directory.absolutePath, 1, COLUMN_WIDTH)
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
                directory = fileChooser.selectedFile
                folderName.text = directory.absolutePath
                // TODO(sonjoonho): This.
                // prefs.putRGCTransductionPref("folderName", directory.absolutePath)
            }
        }
    }
}
