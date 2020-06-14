package simplecolocalization.commands.batch.views.common

import java.awt.Container
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/** Adds a spinner with the given label and model to the container, returning the JSpinner. */
fun addSpinner(container: Container, labelName: String, model: SpinnerNumberModel): JSpinner {
    val panel = JPanel()
    val label = JLabel(labelName)
    val spinner = JSpinner(model)
    label.labelFor = spinner
    panel.add(label)
    panel.add(spinner)
    container.add(panel)
    return spinner
}
