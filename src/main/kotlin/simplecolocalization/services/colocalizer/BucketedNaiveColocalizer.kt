package simplecolocalization.services.colocalizer

import kotlin.math.ceil

/**
 * Runs [NaiveColocalizer], splitting up the positioned cells into buckets and
 * performing naive colocalization on buckets in order to improve performance.
 */
class BucketedNaiveColocalizer(val width: Int, val height: Int, threshold: Float = 0.5f) : NaiveColocalizer(threshold) {
    companion object {
        const val BUCKET_SCALE_FACTOR = 5
    }

    /**
     * Returns a list of transduced cells which overlap target cells and a
     * separate list of transduced cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis {
        // 1. Infer the square bucket size, which corresponds to the diameter
        //    of a cell, bucketLength.
        val bucketLength = inferBucketLengthFromCells(listOf(targetCells, transducedCells).flatten())

        // 2. Split up target cells into buckets, where the key of a bucket is
        //    Pair<a, b>, where the top-left coordinates of the bucket are
        //    (a * bucketLength, b * bucketLength).
        val buckets = HashMap<Pair<Int, Int>, MutableSet<PositionedCell>>()
        for (x in 0 until ceil(width / bucketLength.toDouble()).toInt()) {
            for (y in 0 until ceil(height / bucketLength.toDouble()).toInt()) {
                buckets[Pair(x, y)] = hashSetOf()
            }
        }

        targetCells.forEach { cell -> cell.points.forEach { point -> buckets[bucketForPoint(bucketLength, point)]!!.add(cell) } }

        // 3. For every single transduced cell bucket, construct a list of target
        //    cells in the surrounding buckets, and perform transduction
        //    analysis.
        val transductionAnalyses = transducedCells.map { cell ->
            val surroundingBuckets = cell.points.toHashSet().flatMap { p -> surroundingBucketsForPoint(bucketLength, p) }
            val surroundingTargetCells = surroundingBuckets.flatMap { b -> buckets[b]!!.toList() }.toSet().toList()
            super.analyseTransduction(surroundingTargetCells, listOf(cell))
        }

        // 4. Reconcile the transduction results.
        val overlapping = transductionAnalyses.flatMap { analysis -> analysis.overlapping }.toHashSet()
        val disjoint = transductionAnalyses.flatMap { analysis -> analysis.disjoint }.toHashSet() subtract overlapping
        return TransductionAnalysis(overlapping.toList(), disjoint.toList())
    }

    private fun surroundingBucketsForPoint(bucketLength: Int, point: Pair<Int, Int>): Set<Pair<Int, Int>> {
        val buckets = HashSet<Pair<Int, Int>>()
        for (x in -1..1) {
            for (y in -1..1) {
                buckets.add(Pair(point.first + x, point.second + y))
            }
        }
        return buckets.filter { p -> p.first >= 0 && p.second >= 0 && p.first < (width / bucketLength.toDouble()).toInt() && p.second < (height / bucketLength.toDouble()).toInt() }.toSet()
    }

    private fun bucketForPoint(bucketLength: Int, point: Pair<Int, Int>): Pair<Int, Int> {
        return Pair((point.first / bucketLength.toDouble()).toInt(), (point.second / bucketLength.toDouble()).toInt())
    }

    /**
     * Finding the diameter of a [PositionedCell] given a set of coordinates
     * would be computationally heavy and coordinates being contiguous is not
     * guaranteed, hence we take the heuristic of using taking the number of
     * pixels within a cell as its "diameter" (i.e., line length if all the
     * points were laid out in a line).
     */
    private fun inferBucketLengthFromCells(cells: List<PositionedCell>): Int {
        // return cells.map { cell -> Math.min(cell.points.size * BUCKET_SCALE_FACTOR, Math.min(width, height)) }.max()
        //     ?: Math.min(width, height)
        return 50
    }
}
