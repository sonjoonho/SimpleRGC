package simplecolocalization.commands.batch.views.common

import java.awt.Container
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel

fun addMessage(container: Container, labelName: String) {
    val panel = JPanel()
    panel.layout = GridLayout(0, 1)
    val label = JLabel(labelName)
    panel.add(label)
    container.add(panel)
}
