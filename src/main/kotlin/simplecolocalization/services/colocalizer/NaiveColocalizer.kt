package simplecolocalization.services.colocalizer

import simplecolocalization.services.cellcomparator.CellComparator

open class NaiveColocalizer(private val cellComparator: CellComparator) :
    Colocalizer(cellComparator) {

    /**
     * Returns a list of transduced cells which overlap target cells and a
     * separate list of transduced cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseTransduction(
        targetCells: List<PositionedCell>,
        transducedCells: List<PositionedCell>
    ): TransductionAnalysis {
        val overlap = transducedCells.filter { transducedCell ->
            targetCells.any { cellComparator.cellsOverlap(transducedCell, it) }
        }

        return TransductionAnalysis(overlap, transducedCells.minus(overlap))
    }
}
