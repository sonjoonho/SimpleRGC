package simplecolocalization.services.colocalizer

/**
 * Transduction analysis contains the list of transduced cells which overlap target cells and a list of transduced
 * cells which do not overlap target cells.
 */
data class TransductionAnalysis(val overlapping: List<PositionedCell>, val disjoint: List<PositionedCell>)

/**
 * Analyses the colocalization between cells which are intended to be the
 * target of a vector and the cells which have actually been transduced.
 */
interface Colocalizer {
    /**
     * Returns a list of transduced cells which overlap target cells and separately, a list of transduced cells which
     * do not overlap target cells.
     */
    fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis
}
