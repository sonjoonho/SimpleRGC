package simplergc.commands.batch.views.common

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class RGCSpinner(labelName: String, model: SpinnerNumberModel) : JPanel() {
    var value: Int = 0
    init {
        this.layout = GridLayout(0, 2)
        val label = JLabel(labelName)
        val spinnerPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        val spinner = JSpinner(model)
        value = spinner.value as Int
        spinner.addChangeListener {
            value = spinner.value as Int
        }
        (spinner.editor as JSpinner.DefaultEditor).textField.columns = 20
        label.labelFor = spinner
        this.add(label)
        spinnerPanel.add(spinner, gbc)
        this.add(spinnerPanel)
    }
}
