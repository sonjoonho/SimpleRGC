package simplecolocalization.services.cellcomparator

import kotlin.math.pow
import kotlin.math.sqrt
import simplecolocalization.services.colocalizer.PositionedCell

class DistanceCellComparator(private val distance: Float) : CellComparator {

    private fun distanceBetween(first: Pair<Double, Double>, second: Pair<Double, Double>): Double {
        return sqrt((first.first - second.first).pow(2) + (first.second - second.second).pow(2))
    }

    override fun cellsOverlap(firstCell: PositionedCell, secondCell: PositionedCell): Boolean {
        return distanceBetween(firstCell.center, secondCell.center) <= distance
    }
}
