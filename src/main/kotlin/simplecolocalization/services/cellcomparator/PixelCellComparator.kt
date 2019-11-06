package simplecolocalization.services.cellcomparator

import simplecolocalization.services.colocalizer.PositionedCell

class PixelCellComparator(val threshold: Float) : CellComparator {

    init {
        require(threshold in 0.0..1.0) { "Overlap threshold must be between 0 and 1" }
    }

    override fun cellsOverlap(firstCell: PositionedCell, secondCell: PositionedCell) : Boolean {
        val overlap = (firstCell.points intersect secondCell.points).size.toFloat()
        val total = (firstCell.points union secondCell.points).size.toFloat()
        return (overlap / total) > threshold
    }
}