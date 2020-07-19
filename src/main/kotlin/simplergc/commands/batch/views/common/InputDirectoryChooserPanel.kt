package simplergc.commands.batch.views.common

import java.awt.Component
import java.awt.GridLayout
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class InputDirectoryChooserPanel(container: JPanel, initial: String) : JPanel() {

    var directory = File(initial)

    init {
        this.layout = GridLayout(0, 2)
        val label = ParameterLabel("Input folder")
        label.border = EmptyBorder(0, 0, 0, 10)
        this.add(label)

        val chooserPanel = FileChooserPanel(directory)

        this.add(chooserPanel)

        chooserPanel.browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (fileChooser.showOpenDialog(container) == JFileChooser.APPROVE_OPTION) {
                directory = fileChooser.selectedFile
                chooserPanel.path.text = directory.absolutePath
                // TODO(sonjoonho): This.
                // prefs.putRGCTransductionPref("folderName", directory.absolutePath)
            }
        }
    }
}
