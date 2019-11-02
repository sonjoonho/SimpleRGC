package simplecolocalization.services.colocalizer

import ij.gui.Roi

/**
 * The representation of a positioned cell is a set of points on a
 * two-dimensional coordinate system belonging which form the cell.
 */
class PositionedCell(val points: Set<Pair<Float, Float>>) {

    companion object {
        fun fromRoi(roi: Roi): PositionedCell {
            return PositionedCell(roi.containedPoints.map { point ->
                Pair(point.x.toFloat(), point.y.toFloat())
            }.toSet())
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
}
