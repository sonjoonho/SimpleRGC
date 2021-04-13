package simplergc.commands.batch.views.common

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

class FileChooserPanel(initial: File, onEdit: ((String) -> Unit)? = null) : JPanel() {

    val browseButton = JButton("Browse")
    val path = JTextField(initial.absolutePath, COLUMN_WIDTH)

    init {
        this.layout = GridBagLayout()

        val c = GridBagConstraints()
        c.weightx = 0.1
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = Insets(0, 0, 0, 5)

        this.add(path, c)
        this.add(browseButton)

        if (onEdit != null) {
            path.document.addUndoableEditListener {
                onEdit(path.text)
            }
        }
    }
}
