package simplergc.commands.batch.views.common

import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTextField

class CellDiameterField(initial: String) : JPanel() {
    val field = JTextField()
    init {
        this.layout = GridLayout(0, 2)
        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.X_AXIS)
        val label = ParameterLabel("Cell diameter (px)")
        field.maximumSize = Dimension(Integer.MAX_VALUE, TEXT_FEILD_MAX_HEIGHT)
        field.text = initial

        field.horizontalAlignment = JTextField.RIGHT
        field.toolTipText = "Used as minimum/maximum diameter when identifying cells"
        this.add(label)
        textPanel.add(field)
        this.add(textPanel)
    }
}
