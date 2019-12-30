package simplecolocalization.services.colocalizer

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import simplecolocalization.services.cellcomparator.PixelCellComparator

class BucketedNaiveColocalizerTest : FreeSpec() {
    init {
        "Analyse Bucketed Targeting" - {
            listOf(
                row(
                    "top left cells overlap",
                    0f,
                    listOf(squareCell(0, 3, 0, 3), squareCell(6, 7, 6, 7)),
                    listOf(squareCell(2, 5, 2, 5), squareCell(0, 1, 6, 7)),
                    ColocalizationAnalysis(listOf(squareCell(2, 5, 2, 5)), listOf(squareCell(0, 1, 6, 7)))
                )
            ).map { (description: String, threshold: Float, target: List<PositionedCell>, transduced: List<PositionedCell>, expected: ColocalizationAnalysis) ->
                description {
                    BucketedNaiveColocalizer(2, 8, 8, PixelCellComparator(threshold)).analyseColocalization(target, transduced) shouldBe expected
                }
            }
        }
    }

    companion object {
        fun squareCell(startX: Int, endX: Int, startY: Int, endY: Int): PositionedCell {
            val points = HashSet<Pair<Int, Int>>()
            for (x in startX..endX) {
                for (y in startY..endY) {
                    points.add(Pair(x, y))
                }
            }
            return PositionedCell(points)
        }
    }
}
