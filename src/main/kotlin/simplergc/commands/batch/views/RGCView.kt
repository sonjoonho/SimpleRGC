package simplergc.commands.batch.views

import ij.IJ
import ij.gui.MessageDialog
import javax.swing.JButton
import javax.swing.JPanel
import simplergc.commands.batch.controllers.RGCController

abstract class RGCView : JPanel() {
    val okButton = JButton("Ok")

    abstract fun addListeners(controller: RGCController)

    fun dialog(title: String, body: String) {
        MessageDialog(IJ.getInstance(), title, body)
    }
}
