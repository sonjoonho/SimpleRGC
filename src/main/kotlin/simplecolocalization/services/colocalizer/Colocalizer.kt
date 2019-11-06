package simplecolocalization.services.colocalizer

import simplecolocalization.services.cellcomparator.CellComparator

/**
 * Transduction analysis contains the list of transduced cells which overlap target cells and a list of transduced
 * cells which do not overlap target cells.
 */
data class TransductionAnalysis(val overlapping: List<PositionedCell>, val disjoint: List<PositionedCell>)

/**
 * Analyses the colocalization between cells which are intended to be the
 * target of a vector and the cells which have actually been transduced.
 */
abstract class Colocalizer(private val cellComparator: CellComparator) {

    /**
     * Return true if the target cell colocalises with the tranduced cell,
     * delegating to the CellComparator for the Colocalizer
     */
    fun cellsOverlap(targetCell: PositionedCell, transducedCell: PositionedCell): Boolean {
        return cellComparator.cellsOverlap(targetCell, transducedCell)
    }
    /**
     * Returns a list of transduced cells which overlap target cells and separately, a list of transduced cells which
     * do not overlap target cells.
     */
    abstract fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis
}
