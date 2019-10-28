package simplecolocalization.services.colocalizer

/**
 * Analyses the colocalization between cells which are intended to be the
 * target of a vector and the cells which have actually been transduced.
 */
interface Colocalizer {
    /**
     * Returns an array of transduced cells which overlap target cells.
     * Conversely, the output will not contain transduced cells which do not
     * overlap the target cells.
     */
    fun analyseTargeting(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): List<PositionedCell>
}
