package simplecolocalization.services.cellcomparator
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row
import simplecolocalization.services.colocalizer.PositionedCell

class PixelCellComparatorTest : FreeSpec({
    "Analyse Pixel-based Cell Comparisons" - {
        listOf(
            row(
                "zero threshold matches all cells",
                0f,
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                PositionedCell(hashSetOf(Pair(0, 0))),
                true
            ),
            row(
                "one threshold matches no cells",
                1f,
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                PositionedCell(hashSetOf(Pair(0, 0))),
                false
            ),
            row(
                "transduced cell overlaps target cells",
                0.2f,
                PositionedCell(hashSetOf(Pair(0, 1))),
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                true
            ),
            row(
                "transduced cell does not overlap target cell",
                0f,
                PositionedCell(hashSetOf(Pair(0, 1))),
                PositionedCell(hashSetOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1))),
                true
            )
        ).map { (description: String, distance: Float, target: PositionedCell, transduced: PositionedCell, expected: Boolean) ->
            description {
                PixelCellComparator(distance).cellsOverlap(target, transduced) shouldBe expected
            }
        }
    }
})
