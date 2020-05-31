package simplecolocalization.services.cellcomparator

import simplecolocalization.services.colocalizer.PositionedCell

/**
 * Determines whether two cells overlap using the percentage overlap of pixels in the
 * case where one cell is smaller than the other.
 *
 * If the proportion is larger than the threshold then cells are considered overlapping.
 */
class SubsetPixelCellComparator(private val threshold: Float = 0.5f) : CellComparator {

    init {
        require(threshold in 0.0..1.0) { "Overlap threshold must be between 0 and 1" }
    }

    /**
     * @property firstCell The smaller cell.
     * @property secondCell The larger cell.
     */
    override fun cellsOverlap(firstCell: PositionedCell, secondCell: PositionedCell): Boolean {
        val overlap = (firstCell.points intersect secondCell.points).size.toFloat()
        val total = secondCell.points.size.toFloat()
        return (overlap / total) > threshold
    }
}
