package simplecolocalization.services.colocalizer

/**
 * The representation of a positioned cell is a set of points on a
 * two-dimensional coordinate system belonging which form the cell.
 */
data class PositionedCell(val points: Set<Pair<Float, Float>>)
