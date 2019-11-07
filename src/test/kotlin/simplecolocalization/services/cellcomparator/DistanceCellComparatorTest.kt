package simplecolocalization.services.cellcomparator
import simplecolocalization.services.colocalizer.PositionedCell

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class DistanceCellComparatorTest : FreeSpec({
    "Analyse Distance-based Cell Comparisons" - {
        listOf(
            row(
                "zero distance matches cell only if centre is the same",
                0f,
                PositionedCell(hashSetOf(Pair(0, 0))),
                PositionedCell(hashSetOf(Pair(0, 0))),
                true
            ),
            row(
                "distance matches cell when in range",
                1f,
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                PositionedCell(hashSetOf(Pair(0, 0))),
                true
            ),
            row(
                "distance does not match cell when not in range",
                0.2f,
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                PositionedCell(hashSetOf(Pair(0, 0))),
                false
            ),
            row(
                "distance matches cell when in large range",
                10.0f,
                PositionedCell(hashSetOf(Pair(0, 1))),
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                true
            )
        ).map { (description: String, distance: Float, target: PositionedCell, transduced: PositionedCell, expected: Boolean) ->
            description {
                DistanceCellComparator(distance).cellsOverlap(target, transduced) shouldBe expected
            }
        }
    }
})
