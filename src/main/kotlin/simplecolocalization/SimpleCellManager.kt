package simplecolocalization

import ij.ImagePlus
import simplecolocalization.services.colocalizer.PositionedCell

class SimpleCellManager {
    private val cells: MutableCollection<PositionedCell> = mutableListOf()

    fun add(cell: PositionedCell) {
        cells.add(cell)
    }

    fun clear() {
        cells.clear()
    }

    fun show(imp: ImagePlus) {
        TODO("Unimplemented")
    }
}