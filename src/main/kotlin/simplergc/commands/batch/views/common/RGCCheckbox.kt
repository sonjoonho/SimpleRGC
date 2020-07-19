package simplergc.commands.batch.views.common

import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

/** Adds a checkbox with the given label to the container, returning the JCheckBox. */
class RGCCheckbox(labelName: String, initial: Boolean) : JPanel() {
    var isSelected: Boolean
    init {
        this.layout = GridLayout(0, 2)
        val label = ParameterLabel(labelName)
        val checkbox = JCheckBox()

        checkbox.isSelected = initial
        isSelected = checkbox.isSelected
        checkbox.addActionListener {
            isSelected = checkbox.isSelected
        }
        label.labelFor = checkbox
        this.add(label)
        this.add(checkbox)
    }
}
