package simplecolocalization

import ij.ImagePlus
import ij.gui.Roi
import ij.plugin.frame.RoiManager

class DummyRoiManager : RoiManager(false) {

    private val rois = mutableListOf<Roi>()

    override fun add(imp: ImagePlus?, roi: Roi?, n: Int) {
        // Honestly not sure why this is necessary, but breaks without it. It looks like ROIs are mutated elsewhere -
        // but it's impossible to tell where from!
        rois.add(roi!!.clone() as Roi)
    }

    override fun getRoisAsArray(): Array<Roi> {
        return rois.toTypedArray()
    }
}
