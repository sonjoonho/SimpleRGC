package simplergc.commands.batch.views.common

import java.awt.GridLayout
import java.util.prefs.Preferences
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.getRGCTransductionPref

/** Adds a checkbox with the given label to the container, returning the JCheckBox. */
class RGCCheckbox(labelName: String, prefs: Preferences, prefKey: String, isCounter: Boolean): JPanel() {
    var isSelected: Boolean
    init {
        this.layout = GridLayout(0, 2)
        val label = JLabel(labelName)
        val checkBox = JCheckBox()
        if (isCounter) {
            checkBox.isSelected = prefs.getRGCCounterPref(prefKey, false)
        } else {
            checkBox.isSelected = prefs.getRGCTransductionPref(prefKey, false)
        }
        isSelected = isCounter
        checkBox.addActionListener {
            isSelected = checkBox.isSelected
        }
        label.labelFor = checkBox
        this.add(label)
        this.add(checkBox)
    }
}
