package simplecolocalization.services.colocalizer

import ij.ImagePlus
import ij.gui.Overlay
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.plugin.frame.RoiManager

/**
 * The representation of a positioned cell is a set of points on a
 * two-dimensional coordinate system belonging which form the cell.
 */
class PositionedCell(val points: Set<Pair<Int, Int>>, val outline: Set<Pair<Int, Int>>? = null, val originalImageJRoi: Roi? = null) {

    val center: Pair<Double, Double>

    init {
        var xSum = 0.0
        var ySum = 0.0
        points.forEach { point ->
            xSum += point.first
            ySum += point.second
        }
        center = Pair(xSum / points.size, ySum / points.size)
    }

    fun getMeanIntensity(grayScaleImage: ImagePlus): Float {
        // ImagePlus.getPixel returns size 4 array
        // for grayscale, intensity will be at index 0
        return points.map { grayScaleImage.getPixel(it.first, it.second)[0].toFloat() }.sum() / points.size
    }

    companion object {
        fun fromRoi(roi: Roi): PositionedCell {
            return PositionedCell(
                roi.containedPoints.map { Pair(it.x, it.y) }.toSet(),
                (roi.floatPolygon.xpoints.map { it.toInt() } zip roi.floatPolygon.ypoints.map { it.toInt() }).toSet(),
                originalImageJRoi = roi
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PositionedCell

        if (points != other.points) return false

        return true
    }

    override fun hashCode(): Int {
        return points.hashCode()
    }

    fun toRoi(): Roi {
        if (originalImageJRoi != null) {
            return originalImageJRoi
        }

        if (outline == null) {
            throw RuntimeException("Cannot convert PositionedCell to ImageJ ROI: no cell outline provided.")
        }

        return PolygonRoi(
            outline.map { it.first }.toIntArray(),
            outline.map { it.second }.toIntArray(),
            outline.size,
            Roi.TRACED_ROI
        )
    }
}

/**
 * Adds the given cells to the RoiManager, and displays them on the current active image without their labels.
 * The RoiManager is a singleton, and terribly written; so take care when using this function (or when doing anything
 * related to the RoiManager).
 */
fun addToRoiManager(cells: List<PositionedCell>) {
    val roiManager = RoiManager.getRoiManager()
    roiManager.runCommand("show all with labels")
    cells.forEach { roiManager.addRoi(it.toRoi()) }
}

/**
 * Draws the given cells on the given image. This method does not add the ROIs to the RoiManager.
 */
fun drawCells(imp: ImagePlus, cells: List<PositionedCell>) {
    val rois = cells.map { it.toRoi() }
    val overlay = Overlay()
    rois.forEach { overlay.add(it) }
    val ic = imp.canvas
    if (ic == null) {
        imp.overlay = overlay
        return
    }
    ic.showAllList = overlay
    imp.draw()
}
