package simplecolocalization.commands.batch.views.common

import java.awt.Container
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/** Adds a spinner with the given label and model to the container, returning the JSpinner. */
fun addSpinner(container: Container, labelName: String, model: SpinnerNumberModel): JSpinner {
    val panel = JPanel()
    panel.layout = GridLayout(0, 2)
    val label = JLabel(labelName)
    val spinnerPanel = JPanel(GridBagLayout())
    val gbc = GridBagConstraints()
    val spinner = JSpinner(model)
    (spinner.editor as JSpinner.DefaultEditor).textField.columns = 20
    label.labelFor = spinner
    panel.add(label)
    spinnerPanel.add(spinner, gbc)
    panel.add(spinnerPanel)
    container.add(panel)
    return spinner
}
