package simplecolocalization.commands

import ij.gui.GenericDialog

fun String.displayOutputFileErrorDialog() {
    GenericDialog("Error").apply {
        addMessage("Unable to save results to $this file. Ensure the output file is not currently open by other programs and try again.")
        hideCancelButton()
        showDialog()
    }
}