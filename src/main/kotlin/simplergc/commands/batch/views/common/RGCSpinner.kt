package simplergc.commands.batch.views.common

import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

const val TEXT_FEILD_MAX_HEIGHT = 20

class RGCSpinner(labelName: String, model: SpinnerNumberModel) : JPanel() {
    var value: Int = 0
    init {
        this.layout = GridLayout(0, 2)
        val label = ParameterLabel(labelName)
        val spinnerPanel = JPanel()
        spinnerPanel.layout = BoxLayout(spinnerPanel, BoxLayout.X_AXIS)
        val spinner = JSpinner(model)
        spinner.maximumSize = Dimension(Integer.MAX_VALUE, TEXT_FEILD_MAX_HEIGHT)
        value = spinner.value as Int
        spinner.addChangeListener {
            value = spinner.value as Int
        }
        label.labelFor = spinner
        this.add(label)
        spinnerPanel.add(spinner)
        this.add(spinnerPanel)
    }
}
