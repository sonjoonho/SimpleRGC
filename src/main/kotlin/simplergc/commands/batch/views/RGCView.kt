package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import simplergc.commands.batch.controllers.RGCController
import java.awt.event.WindowEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

abstract class RGCView(private val frame: JFrame) : JPanel() {
    val okButton = JButton("OK")

    abstract fun addListeners(controller: RGCController)

    fun dialog(title: String, body: String) {
        MessageDialog(IJ.getInstance(), title, body)
    }

    fun close() {
        frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
    }
}
