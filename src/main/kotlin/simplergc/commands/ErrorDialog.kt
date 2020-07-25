package simplergc.commands

import ij.gui.GenericDialog

fun displayErrorDialog(message: String?) {
    GenericDialog("Error").apply {
        addMessage(message ?: "An error occurred. Please try again.")
        hideCancelButton()
        showDialog()
    }
}
