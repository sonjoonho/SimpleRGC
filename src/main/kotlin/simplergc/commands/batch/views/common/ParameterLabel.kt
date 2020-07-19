package simplergc.commands.batch.views.common

import javax.swing.JLabel
import javax.swing.border.EmptyBorder

class ParameterLabel(label: String) : JLabel(label, RIGHT) {

    init {
        this.border = EmptyBorder(0, 0, 0, 10)
    }
}
