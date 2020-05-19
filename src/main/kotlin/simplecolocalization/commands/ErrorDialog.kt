package simplecolocalization.commands

import ij.gui.GenericDialog

fun displayOutputFileErrorDialog(filetype: String = "") {
    GenericDialog("Error").apply {
        addMessage("Unable to save results to $filetype file. Ensure the output file is not currently open by other programs and try again.")
        hideCancelButton()
        showDialog()
    }
}