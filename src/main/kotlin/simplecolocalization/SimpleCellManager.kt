package simplecolocalization

import ij.ImagePlus
import ij.gui.Overlay
import ij.gui.Roi
import ij.plugin.OverlayLabels
import simplecolocalization.services.colocalizer.PositionedCell

class SimpleCellManager {
    val cells: MutableList<PositionedCell> = mutableListOf()

    fun add(cell: PositionedCell) {
        cells.add(cell)
    }

    fun clear() {
        cells.clear()
    }

    fun show(imp: ImagePlus) {
        val rois: Collection<Roi> = cells.map { it.toRoi() }
        val overlay: Overlay = OverlayLabels.createOverlay()
        rois.forEach { overlay.add(it) }
        val ic = imp.canvas
        imp.overlay = overlay
        ic.showAllList = overlay
        imp.draw()
    }
}
