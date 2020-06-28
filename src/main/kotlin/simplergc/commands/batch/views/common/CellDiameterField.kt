package simplergc.commands.batch.views.common

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CellDiameterField(initial: String) : JPanel() {
    val field = JTextField()
    init {
        this.layout = GridLayout(0, 2)
        val textPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        val label = JLabel("Cell diameter (px)")
        field.text = initial

        field.horizontalAlignment = JTextField.RIGHT
        field.columns = 22
        field.toolTipText = "Used as minimum/maximum diameter when identifying cells"
        this.add(label)
        textPanel.add(field, gbc)
        this.add(textPanel)
    }
}
