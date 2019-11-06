package simplecolocalization.services.colocalizer

import simplecolocalization.services.cellcomparator.CellComparator
import kotlin.math.ceil
import kotlin.math.min

/**
 * Runs [NaiveColocalizer], splitting up the positioned cells into buckets and
 * performing naive colocalization on buckets in order to improve performance.
 */
class BucketedNaiveColocalizer(val bucketLength: Int, val width: Int, val height: Int, cellComparator: CellComparator) : NaiveColocalizer(cellComparator) {

    /**
     * Returns a list of transduced cells which overlap target cells and a
     * separate list of transduced cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis {
        // 1. Infer the square bucket size, which should be around the size of
        //    a typical cell in pixels.
        val bucketLength = min(this.bucketLength, min(width, height))

        // 2. Split up target cells into buckets, where the key of a bucket is
        //    Pair<a, b>, where the top-left coordinates of the bucket are
        //    (a * bucketLength, b * bucketLength).
        val buckets = HashMap<Pair<Int, Int>, MutableSet<PositionedCell>>()
        for (x in 0 until ceil(width / bucketLength.toDouble()).toInt()) {
            for (y in 0 until ceil(height / bucketLength.toDouble()).toInt()) {
                buckets[Pair(x, y)] = hashSetOf()
            }
        }

        targetCells.forEach { cell -> cell.points.forEach { point -> buckets[bucketForPoint(point)]!!.add(cell) } }

        // 3. For every single transduced cell bucket, construct a list of target
        //    cells in the surrounding buckets, and perform transduction
        //    analysis.
        val transductionAnalyses = transducedCells.map { transducedCell ->
            val surroundingTargetCells = transducedCell.points.toHashSet().flatMap { p -> surroundingBucketsForBucket(bucketForPoint(p)) }.flatMap { b -> buckets[b]!! }.toList()
            super.analyseTransduction(surroundingTargetCells, listOf(transducedCell))
        }

        // 4. Reconcile the transduction results.
        val overlapping = transductionAnalyses.flatMap { analysis -> analysis.overlapping }.toHashSet()
        val disjoint = transductionAnalyses.flatMap { analysis -> analysis.disjoint }.toHashSet() subtract overlapping
        return TransductionAnalysis(overlapping.toList(), disjoint.toList())
    }

    private fun surroundingBucketsForBucket(bucket: Pair<Int, Int>): Set<Pair<Int, Int>> {
        val buckets = HashSet<Pair<Int, Int>>()
        for (x in -1..1) {
            for (y in -1..1) {
                buckets.add(Pair(bucket.first + x, bucket.second + y))
            }
        }
        return buckets.filter { p -> p.first >= 0 && p.second >= 0 && p.first < (width / bucketLength.toDouble()).toInt() && p.second < (height / bucketLength.toDouble()).toInt() }.toSet()
    }

    private fun bucketForPoint(point: Pair<Int, Int>): Pair<Int, Int> {
        return Pair((point.first / bucketLength.toDouble()).toInt(), (point.second / bucketLength.toDouble()).toInt())
    }
}
