package simplecolocalization.services

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class CellColocalizationServiceTest : StringSpec({
    "counts cells correctly" {
        val cellColocalizationService = CellColocalizationService()
        val analyses = arrayOf(
            CellColocalizationService.CellAnalysis(5, 100, 100, 500),
            CellColocalizationService.CellAnalysis(20, 88, 88, 1760),
            CellColocalizationService.CellAnalysis(20, 10, 10, 200),
            CellColocalizationService.CellAnalysis(20, 15, 15, 300)
        )

        cellColocalizationService.countChannel(analyses, 30.0) shouldBe 2
    }
})
