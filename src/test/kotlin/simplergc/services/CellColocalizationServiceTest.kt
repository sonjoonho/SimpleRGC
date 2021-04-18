package simplergc.services

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class CellColocalizationServiceTest : StringSpec({
    "counts cells correctly" {
        val cellColocalizationService = CellColocalizationService()
        val analyses = arrayOf(
            CellColocalizationService.CellAnalysis(5, 100.0, 100, 100, 500, 1000),
            CellColocalizationService.CellAnalysis(20, 88.0, 88, 88, 1760, 3000),
            CellColocalizationService.CellAnalysis(20, 10.0, 10, 10, 200, 1000),
            CellColocalizationService.CellAnalysis(20, 15.0, 15, 15, 300, 1000)
        )

        cellColocalizationService.countChannel(analyses, 30.0) shouldBe 2
    }
})
