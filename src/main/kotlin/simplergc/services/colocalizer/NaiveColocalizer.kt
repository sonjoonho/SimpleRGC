package simplergc.services.colocalizer

import simplergc.services.cellcomparator.CellComparator

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
        val overlaidBaseOverlap = overlaidCells.map { overlaidCell ->
            overlaidCell to baseCells.filter { isOverlapping(overlaidCell, it) }
        }.toMap()

        val overlappingBaseCells = overlaidBaseOverlap.values.flatten().toSet()
        val overlappingOverlaidCells = overlaidBaseOverlap.filter { it.value.isNotEmpty() }.keys

        return ColocalizationAnalysis(
            overlappingBase = overlappingBaseCells.toList(),
            overlappingOverlaid = overlappingOverlaidCells.toList(),
            disjoint = overlaidCells.minus(overlappingOverlaidCells)
        )
    }
}
