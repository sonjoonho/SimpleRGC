package simplecolocalization.comparators

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class AlphanumComparatorTest : FreeSpec({
    "Orders strings correctly" - {
        listOf(
            row(
                "characters without numbers",
                mutableListOf("a", "aa", "b", "bb").shuffled(),
                mutableListOf("a", "aa", "b", "bb")
            ),
            row(
                "numbers",
                mutableListOf("1", "2", "10").shuffled(),
                mutableListOf("1", "2", "10")
            ),
            row(
                "characters with numbers",
                mutableListOf("a1", "a2", "a10").shuffled(),
                mutableListOf("a1", "a2", "a10")
            ),
            row(
                "characters either side of numbers",
                mutableListOf("a1b", "a2b", "a10b").shuffled(),
                mutableListOf("a1b", "a2b", "a10b")
            )
        ).map { (description: String, unsorted: Collection<String>, sorted: Collection<String>) ->
            description {
                unsorted.sortedWith(AlphanumComparator) shouldBe sorted
            }
        }
    }
})
