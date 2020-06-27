package simplergc.commands.batch.views.common

import java.awt.Container
import java.awt.GridLayout
import java.util.prefs.Preferences
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.getRGCTransductionPref

/** Adds a checkbox with the given label to the container, returning the JCheckBox. */
fun addCheckBox(container: Container, labelName: String, prefs: Preferences, prefKey: String, isCounter: Boolean): JCheckBox {
    val panel = JPanel()
    panel.layout = GridLayout(0, 2)
    val label = JLabel(labelName)
    val checkBox = JCheckBox()
    if (isCounter) {
        checkBox.isSelected = prefs.getRGCCounterPref(prefs, prefKey, false)
    } else {
        checkBox.isSelected = prefs.getRGCTransductionPref(prefs, prefKey, false)
    }
    label.labelFor = checkBox
    panel.add(label)
    panel.add(checkBox)
    container.add(panel)
    return checkBox
}
