package simplecolocalization.services.colocalizer

import kotlin.math.ceil
import kotlin.math.min

/**
 * Runs [NaiveColocalizer], splitting up the positioned cells into buckets and
 * performing naive colocalization on buckets in order to improve performance.
 */
class BucketedNaiveColocalizer(var bucketLength: Int, val imageWidth: Int, val imageHeight: Int, threshold: Float = 0.5f) : NaiveColocalizer(threshold) {

    init {
        bucketLength = min(this.bucketLength, min(imageWidth, imageHeight))
    }

    /**
     * A bucket is represented by coordinates describing its top-left corner.
     */
    private data class Bucket(val x: Int, val y: Int)

    /**
     * Returns a list of transduced cells which overlap target cells and a
     * separate list of transduced cells which do not. Two cells overlap when
     * the area of their intersection divided by the total area spanned by the
     * cells is greater than the threshold.
     */
    override fun analyseTransduction(targetCells: List<PositionedCell>, transducedCells: List<PositionedCell>): TransductionAnalysis {
        // 2. Split up target cells into buckets, where the key of a bucket is
        //    Pair<a, b>, where the top-left coordinates of the bucket are
        //    (a * bucketLength, b * bucketLength).
        val buckets = HashMap<Bucket, MutableSet<PositionedCell>>()
        for (x in 0 until numBucketsForWidth()) {
            for (y in 0 until numBucketsForHeight()) {
                buckets[Bucket(x, y)] = hashSetOf()
            }
        }

        targetCells.forEach { cell -> cell.points.forEach { point -> buckets[bucketForPoint(point)]!!.add(cell) } }

        // 3. For every single transduced cell bucket, construct a list of target
        //    cells in the surrounding buckets, and perform transduction
        //    analysis.
        val transductionAnalyses = transducedCells.map { transducedCell ->
            val surroundingBuckets = transducedCell.points.toHashSet().flatMap { p -> surroundingBucketsForBucket(bucketForPoint(p)) }
            val surroundingTargetCells = surroundingBuckets.flatMap { b -> buckets[b]!!.toList() }.toSet().toList()
            super.analyseTransduction(surroundingTargetCells, listOf(transducedCell))
        }

        // 4. Reconcile the transduction results.
        val overlapping = transductionAnalyses.flatMap { analysis -> analysis.overlapping }.toHashSet()
        val disjoint = transductionAnalyses.flatMap { analysis -> analysis.disjoint }.toHashSet() subtract overlapping
        return TransductionAnalysis(overlapping.toList(), disjoint.toList())
    }

    private fun surroundingBucketsForBucket(bucket: Bucket): Set<Bucket> {
        val buckets = HashSet<Bucket>()
        for (x in -1..1) {
            for (y in -1..1) {
                buckets.add(Bucket(bucket.x + x, bucket.y + y))
            }
        }
        return buckets.filter { p -> p.x >= 0 && p.y >= 0 && p.x < numBucketsForWidth() && p.y < numBucketsForHeight()}.toSet()
    }

    private fun numBucketsForWidth() : Int {
        return ceil(imageWidth / bucketLength.toDouble()).toInt()
    }

    private fun numBucketsForHeight() : Int {
        return ceil(imageHeight / bucketLength.toDouble()).toInt()
    }

    private fun bucketForPoint(point: Pair<Int, Int>): Bucket {
        return Bucket((point.first / bucketLength.toDouble()).toInt(), (point.second / bucketLength.toDouble()).toInt())
    }
}
