package simplecolocalization.commands.batch.views.common

import java.awt.Container
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

/** Adds a checkbox with the given label to the container, returning the JCheckBox. */
fun addCheckBox(container: Container, labelName: String): JCheckBox {
    val panel = JPanel()
    val label = JLabel(labelName)
    val checkBox = JCheckBox()
    label.labelFor = checkBox
    panel.add(label)
    panel.add(checkBox)
    container.add(panel)
    return checkBox
}
