package simplecolocalization.services

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class CellColocalizationServiceTest : StringSpec({
    "counts cells correctly" {
        val cellColocalizationService = CellColocalizationService()
        val analyses = arrayOf(
            CellColocalizationService.CellAnalysis(5, 100, 100),
            CellColocalizationService.CellAnalysis(20, 88, 88),
            CellColocalizationService.CellAnalysis(20, 10, 10),
            CellColocalizationService.CellAnalysis(20, 15, 15)
        )

        cellColocalizationService.countChannel(analyses, 30.0) shouldBe 2
    }
})
