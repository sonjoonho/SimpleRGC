package simplecolocalization.services.colocalizer

import simplecolocalization.services.cellcomparator.CellComparator

open class NaiveColocalizer(cellComparator: CellComparator) :
    Colocalizer(cellComparator) {

    /**
     * Returns a list of overlaid cells which overlap target cells and a
     * separate list of overlaid cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseColocalization(
        baseCells: List<PositionedCell>,
        overlaidCells: List<PositionedCell>
    ): ColocalizationAnalysis {
        val overlap = overlaidCells.filter { overlaidCell ->
            baseCells.any { isOverlapping(overlaidCell, it) }
        }

        return ColocalizationAnalysis(overlap, overlaidCells.minus(overlap))
    }
}
