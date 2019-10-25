package simplecolocalization.services

import kotlin.test.assertEquals
import org.junit.Test

class CellColocalizationServiceTest {
    @Test
    fun countsCellsCorrectly() {
        val cellColocalizationService = CellColocalizationService()
        val analyses = arrayOf(
            CellSegmentationService.CellAnalysis(
                5,
                listOf(
                    CellSegmentationService.ChannelAnalysis("red", 50, 1, 216),
                    CellSegmentationService.ChannelAnalysis("green", 100, 20, 160),
                    CellSegmentationService.ChannelAnalysis("blue", 10, 40, 59)
                )
            ),
            CellSegmentationService.CellAnalysis(
                20,
                listOf(
                    CellSegmentationService.ChannelAnalysis("red", 150, 1, 216),
                    CellSegmentationService.ChannelAnalysis("green", 88, 40, 160),
                    CellSegmentationService.ChannelAnalysis("blue", 30, 40, 59)
                )
            ),
            CellSegmentationService.CellAnalysis(
                20,
                listOf(
                    CellSegmentationService.ChannelAnalysis("red", 150, 1, 216),
                    CellSegmentationService.ChannelAnalysis("green", 10, 40, 160),
                    CellSegmentationService.ChannelAnalysis("blue", 30, 40, 59)
                )
            ),
            CellSegmentationService.CellAnalysis(
                20,
                listOf(
                    CellSegmentationService.ChannelAnalysis("red", 150, 1, 216),
                    CellSegmentationService.ChannelAnalysis("green", 15, 40, 160),
                    CellSegmentationService.ChannelAnalysis("blue", 30, 40, 59)
                )
            )
        )

        assertEquals(2, cellColocalizationService.countChannel(analyses, 1, 30.0))
    }
}
