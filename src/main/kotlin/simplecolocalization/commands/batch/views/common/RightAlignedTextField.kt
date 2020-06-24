package simplecolocalization.commands.batch.views.common

import java.awt.Container
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

fun addCellDiameterField(container: Container, initial: String = "00.0-30.0"): JTextField {
    val panel = JPanel()
    panel.layout = GridLayout(0, 2)
    val textPanel = JPanel(GridBagLayout())
    val gbc = GridBagConstraints()
    val label = JLabel("Cell diameter (px)")
    val t = JTextField(initial)
    t.horizontalAlignment = JTextField.RIGHT
    t.columns = 20
    panel.add(label)
    textPanel.add(t, gbc)
    panel.add(textPanel)
    container.add(panel)
    return t
}
