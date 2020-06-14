package simplergc.services.cellcomparator

import simplergc.services.colocalizer.PositionedCell

/**
 * Analyses the colocalization/overlap between a cell which is intended to be the
 * target of a vector and a cell which has actually been transduced.
 */

interface CellComparator {
    fun cellsOverlap(firstCell: PositionedCell, secondCell: PositionedCell): Boolean
}
