package simplecolocalization.services.colocalizer

import simplecolocalization.services.cellcomparator.CellComparator

/**
 * Colocalization analysis contains the list of overlaid cells which overlap base cells and a list of overlaid
 * cells which do not overlap base cells.
 */
data class ColocalizationAnalysis(val overlappingBase: List<PositionedCell>, val overlappingOverlaid: List<PositionedCell>, val disjoint: List<PositionedCell>)

/**
 * Analyses the colocalization between cells which are intended to be the
 * target of a vector and the cells which have actually been transduced.
 */
abstract class Colocalizer(private val cellComparator: CellComparator) {

    /**
     * Return true if the target cell colocalises with the transduced cell,
     * delegating to the CellComparator for the Colocalizer
     */
    fun isOverlapping(baseCell: PositionedCell, overlaidCell: PositionedCell): Boolean {
        return cellComparator.cellsOverlap(baseCell, overlaidCell)
    }

    /**
     * Returns a list of transduced cells which overlap target cells and separately, a list of transduced cells which
     * do not overlap target cells.
     */
    abstract fun analyseColocalization(baseCells: List<PositionedCell>, overlaidCells: List<PositionedCell>): ColocalizationAnalysis
}
