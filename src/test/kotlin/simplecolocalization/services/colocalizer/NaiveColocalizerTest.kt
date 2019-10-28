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
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f))))
            ),
            row(
                "one threshold matches no cells",
                1f,
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f), Pair(0f, 1f), Pair(1f, 0f), Pair(1f, 1f)))),
                listOf(PositionedCell(hashSetOf(Pair(0f, 0f)))),
                listOf()
            )
        ).map { (description: String, threshold: Float, target: List<PositionedCell>, transduced: List<PositionedCell>, expected: List<PositionedCell>) ->
            description {
                NaiveColocalizer(threshold).analyseTargeting(target, transduced) shouldBe expected
            }
        }
    }
})

