package simplecolocalization.commands.batch.views.common

import java.awt.Container
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/** Adds a file chooser with the given label to the container, returning the browseButton so an
 *  action listener can be added by the caller.
 */
fun addFileChooser(container: Container, labelName: String): JButton {
    val panel = JPanel()
    val label = JLabel(labelName)
    val browseButton = JButton("Browse")
    label.labelFor = browseButton
    panel.add(label)
    panel.add(browseButton)
    container.add(panel)

    return browseButton
}
