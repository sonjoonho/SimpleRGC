package simplergc.commands.batch.views.common

import java.awt.Container
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.util.prefs.Preferences
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.getRGCTransductionPref

fun addCellDiameterField(container: Container, prefs: Preferences, prefKey: String, isCounter: Boolean): JTextField {
    val panel = JPanel()
    panel.layout = GridLayout(0, 2)
    val textPanel = JPanel(GridBagLayout())
    val gbc = GridBagConstraints()
    val label = JLabel("Cell diameter (px)")
    val initial: String = if (isCounter) {
        prefs.getRGCCounterPref(prefKey, "00.0-30.0")
    } else {
        prefs.getRGCTransductionPref(prefKey, "00.0-30.0")
    }
    val t = JTextField(initial)
    t.horizontalAlignment = JTextField.RIGHT
    t.columns = 22
    t.toolTipText = "Used as minimum/maximum diameter when identifying cells"
    panel.add(label)
    textPanel.add(t, gbc)
    panel.add(textPanel)
    container.add(panel)
    return t
}
