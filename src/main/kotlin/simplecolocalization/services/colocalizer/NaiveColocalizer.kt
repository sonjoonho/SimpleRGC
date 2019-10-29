package simplecolocalization.services.colocalizer

/**
 * Uses a naive method of colocalization: checking to see if there is greater
 * than a specified percentage [threshold] of overlap. Lower threshold is
 * better.
 */
class NaiveColocalizer(private val threshold: Float = 0.5f) : Colocalizer {

    init {
        require(threshold in 0.0..1.0) { "Overlap threshold must be between 0 and 1" }
    }

    /**
     * Returns a list of transduced cells which overlap target cells and a
     * separate list of transduced cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis {
        val overlap = transducedCells.filter { transducedCell ->
            targetCells.any { targetCell ->
                val overlap = (transducedCell.points intersect targetCell.points).size.toFloat()
                val total = (transducedCell.points union targetCell.points).size.toFloat()
                (overlap / total) > threshold
            }
        }

        return TransductionAnalysis(overlap, transducedCells.minus(overlap))
    }
}
