package simplecolocalization.services.colocalizer

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class NaiveColocalizerTest : FreeSpec({
    "Analyse Targeting" - {
        listOf(
            row(
                "zero threshold matches all cells",
                0f,
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f), Pair(0f, 1f), Pair(1f, 0f), Pair(1f, 1f)))),
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f)))),
                TransductionAnalysis(listOf(PositionedCell(hashSetOf(Pair(0f, 0f)))), listOf())
            ),
            row(
                "one threshold matches no cells",
                1f,
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f), Pair(0f, 1f), Pair(1f, 0f), Pair(1f, 1f)))),
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f)))),
                TransductionAnalysis(listOf(), listOf(PositionedCell(hashSetOf(Pair(0f, 0f)))))
            ),
            row(
                "transduced cell overlaps multiple target cells",
                0.2f,
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f))), PositionedCell(hashSetOf(Pair(0f, 1f))), PositionedCell(hashSetOf(Pair(1f, 0f))), PositionedCell(hashSetOf(Pair(1f, 1f)))),
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f), Pair(0f, 1f), Pair(1f, 0f), Pair(1f, 1f)))),
                TransductionAnalysis(listOf(PositionedCell(hashSetOf(Pair(0f, 0f), Pair(0f, 1f), Pair(1f, 0f), Pair(1f, 1f)))), listOf())
            ),
            row(
                "transduced cell does not overlap any target cells",
                0f,
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f))), PositionedCell(hashSetOf(Pair(0f, 1f))), PositionedCell(hashSetOf(Pair(1f, 0f))), PositionedCell(hashSetOf(Pair(1f, 1f)))),
                listOf(PositionedCell(hashSetOf(Pair(2f, 2f)))),
                TransductionAnalysis(listOf(), listOf(PositionedCell(hashSetOf(Pair(2f, 2f)))))
            )
        ).map { (description: String, threshold: Float, target: List<PositionedCell>, transduced: List<PositionedCell>, expected: TransductionAnalysis) ->
            description {
                NaiveColocalizer(threshold).analyseTransduction(target, transduced) shouldBe expected
            }
        }
    }
})
