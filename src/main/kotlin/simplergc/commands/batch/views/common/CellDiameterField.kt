package simplergc.commands.batch.views.common

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.util.prefs.Preferences
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import simplergc.commands.batch.getRGCCounterPref
import simplergc.commands.batch.getRGCTransductionPref

class CellDiameterField(prefs: Preferences, prefKey: String, isCounter: Boolean) : JPanel() {
    val field = JTextField()
    init {
        this.layout = GridLayout(0, 2)
        val textPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        val label = JLabel("Cell diameter (px)")
        val initial: String = if (isCounter) {
            prefs.getRGCCounterPref(prefKey, "00.0-30.0")
        } else {
            prefs.getRGCTransductionPref(prefKey, "00.0-30.0")
        }
        field.text = initial

        field.horizontalAlignment = JTextField.RIGHT
        field.columns = 22
        field.toolTipText = "Used as minimum/maximum diameter when identifying cells"
        this.add(label)
        textPanel.add(field, gbc)
        this.add(textPanel)
    }
}
