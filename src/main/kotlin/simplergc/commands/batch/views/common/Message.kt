package simplergc.commands.batch.views.common

import java.awt.Component
import java.awt.Container
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

fun addMessage(container: Container, labelName: String) {
    val panel = JPanel()
    panel.layout = GridLayout(0, 1)
    val label = ParameterLabel(labelName)
    label.border = EmptyBorder(0, 10, 0, 10)

    panel.add(label)
    container.add(panel)
}

// The difference between addMessage and addLabel is that addMessage spans the
// entire window, whereas addLabel does not.
fun addLabel(container: Container, labelName: String) {
    val panel = JPanel()
    panel.layout = GridLayout(0, 2)
    val label = ParameterLabel(labelName)
    panel.add(label)
    container.add(panel)
}