package simplergc.comparators

import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.tables.row

class AlphanumComparatorTest : FreeSpec({
    "Orders strings correctly" - {
        listOf(
            row(
                "no strings",
                mutableListOf(),
                mutableListOf()
            ),
            row(
                "empty string",
                mutableListOf("", "a", "b").shuffled(),
                mutableListOf("", "a", "b")
            ),
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
            ),
            row(
                "characters before numbers",
                mutableListOf("a", "a2", "b1").shuffled(),
                mutableListOf("a", "a2", "b1")
            ),
            row(
                "special characters",
                mutableListOf("a-b", "a-b&1.", "a-b&2.").shuffled(),
                mutableListOf("a-b", "a-b&1.", "a-b&2.")
            )
        ).map { (description: String, unsorted: Collection<String>, sorted: Collection<String>) ->
            description {
                unsorted.sortedWith(AlphanumComparator) shouldBe sorted
            }
        }
    }
})
