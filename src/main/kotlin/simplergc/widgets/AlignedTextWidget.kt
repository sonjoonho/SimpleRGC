package simplergc.widgets

import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.scijava.Priority
import org.scijava.plugin.Plugin
import org.scijava.ui.swing.widget.SwingInputWidget
import org.scijava.widget.InputWidget
import org.scijava.widget.TextWidget
import org.scijava.widget.WidgetModel

/**
 * Custom text widget which can be used as an @Parameter argument as shown below:
 *      @Parameter(
 *          style = AlignedTextWidget.RIGHT
 *          ...
 *      )
 * Behaves as a regular text field but it is right-aligned.
 * Contains a significant amount of interface implementations, however the important method is
 * [set] which creates and styles the text field.
 *
 */
@Plugin(type = InputWidget::class, priority = Priority.HIGH)
class AlignedTextWidget : SwingInputWidget<String>(),
    DocumentListener, TextWidget<JPanel> {

    companion object {
        // Style constant to use in @Parameter
        const val RIGHT = "right"
    }

    private var textComponent: JTextField? = null

    // DocumentListener interface methods
    override fun changedUpdate(e: DocumentEvent?) {
        updateModel()
    }

    override fun insertUpdate(e: DocumentEvent?) {
        updateModel()
    }

    override fun removeUpdate(e: DocumentEvent?) {
        updateModel()
    }

    // InputWidget method
    override fun getValue(): String? {
        return textComponent!!.text
    }

    // WrapperPlugin methods
    override fun set(model: WidgetModel) {
        super.set(model)
        val columns = model.item.columnCount

        textComponent = JTextField("", columns)
        textComponent!!.horizontalAlignment = JTextField.RIGHT
        setToolTip(textComponent)
        component.add(textComponent)
        textComponent!!.document.addDocumentListener(this)
        refreshWidget()
    }

    // Typed methods
    override fun supports(model: WidgetModel): Boolean {
        return super<SwingInputWidget>.supports(model) && model.isStyle(RIGHT) && model.isText &&
            !model.isMultipleChoice && !model.isMessage
    }

    // AbstractUIInputWidget methods
    override fun doRefresh() {
        val text = get().text
        if (textComponent!!.text == text) return // no change
        textComponent!!.text = text
    }
}
