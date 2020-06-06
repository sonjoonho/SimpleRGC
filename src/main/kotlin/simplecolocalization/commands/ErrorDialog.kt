package simplecolocalization.commands

import ij.gui.GenericDialog

fun displayOutputFileErrorDialog(filetype: String = "") {
    GenericDialog("Error").apply {
        addMessage("Unable to save results to $filetype file")
        hideCancelButton()
        showDialog()
    }
}
