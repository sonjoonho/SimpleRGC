package simplergc.services

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class CellDiameterRangeTest : FreeSpec({
    "Parse valid diameter ranges" - {
        listOf(
            row(
                "normal text with no space",
                "0.0-30.0",
                0.0,
                30.0
            ),
            row(
                "normal text with space",
                "0.0 -   30.0",
                0.0,
                30.0
            ),
            row(
                "two digits after decimal",
                "0.01 - 30.55",
                0.01,
                30.55
            ),
            row(
                "integers",
                "0 - 30",
                0.0,
                30.0
            )
        ).map { (description: String, text: String, smallest: Double, largest: Double) ->
            description {
                val range = CellDiameterRange.parseFromText(text)
                range.smallest shouldBe smallest
                range.largest shouldBe largest
            }
        }
    }
    "Throw parse exceptions" - {
        listOf(
            row(
                "missing largest",
                "0.0 - ",
                "An invalid format for the cell diameter range has been entered. The cell diameter range should be entered in the format '# - #' in which # is a number (up to two decimal places)."
            ),
            row(
                "missing smallest",
                "-30.0",
                "An invalid format for the cell diameter range has been entered. The cell diameter range should be entered in the format '# - #' in which # is a number (up to two decimal places)."
            ),
            row(
                "missing - ",
                "0.0 30.0 ",
                "An invalid format for the cell diameter range has been entered. The cell diameter range should be entered in the format '# - #' in which # is a number (up to two decimal places)."
            ),
            row(
                "invalid decimal",
                "0f0 - 30.0",
                "An invalid format for the cell diameter range has been entered. The cell diameter range should be entered in the format '# - #' in which # is a number (up to two decimal places)."
            ),
            row(
                "contains alpha characters",
                "0.0-30.b ",
                "An invalid format for the cell diameter range has been entered. The cell diameter range should be entered in the format '# - #' in which # is a number (up to two decimal places)."
            ),
            row(
                "smallest equals largest",
                "30.0-30.0",
                "Smallest cell diameter must be smaller than the largest cell diameter"
            ),
            row(
                "smallest larger than largest",
                "30.0-20.0",
                "Smallest cell diameter must be smaller than the largest cell diameter"
            )
        ).map { (description: String, text: String, message: String) ->
            description {
                val exception = shouldThrow<DiameterParseException> { CellDiameterRange.parseFromText(text) }
                exception.message shouldBe message
            }
        }
    }
})
