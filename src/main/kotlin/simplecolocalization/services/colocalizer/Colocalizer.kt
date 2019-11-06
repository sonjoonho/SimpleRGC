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
abstract class Colocalizer(cellComparator: CellComparator) {
    /**
     * Returns a list of transduced cells which overlap target cells and separately, a list of transduced cells which
     * do not overlap target cells.
     */
    abstract fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis
}
