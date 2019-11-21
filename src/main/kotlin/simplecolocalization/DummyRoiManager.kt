package simplecolocalization

import ij.ImagePlus
import ij.gui.Roi
import ij.plugin.frame.RoiManager

class DummyRoiManager : RoiManager(false) {

    private val rois = mutableListOf<Roi>()

    override fun add(imp: ImagePlus?, roi: Roi?, n: Int) {
        rois.add(roi!!)
    }

    override fun getRoisAsArray(): Array<Roi> {
        return rois.toTypedArray()
    }
}