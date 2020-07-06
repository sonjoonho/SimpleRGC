package simplergc.commands.batch.views.common

import javax.swing.JTextArea
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.Document
import javax.swing.text.PlainDocument

class Document(private val columns: Int) : PlainDocument() {

    @Throws(BadLocationException::class)
    override fun insertString(offs: Int, str: String, a: AttributeSet?) {
        super.insertString(offs, str, a)
        if (length > columns) {
            super.remove(3, length - columns + 3)
            super.insertString(0, "...", a)
        }
    }
}

class FileChooserTextArea(text: String, rows: Int, columns: Int) : JTextArea(text, rows, columns) {

    override fun setDocument(doc: Document?) {
        val document = Document(columns)
        super.setDocument(document)
    }
}
