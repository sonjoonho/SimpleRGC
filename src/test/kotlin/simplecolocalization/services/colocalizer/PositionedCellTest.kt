package simplecolocalization.services.colocalizer

import ij.gui.PolygonRoi
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PositionedCellTest : StringSpec({
    "fromRoi and toRoi should be inverse operations" {
        val roi = PolygonRoi(floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f), PolygonRoi.POLYGON)
        val cell = PositionedCell.fromRoi(roi)
        val backToRoi = cell.toRoi()
        roi.polygon.xpoints shouldBe backToRoi.polygon.xpoints
        roi.polygon.ypoints shouldBe backToRoi.polygon.ypoints
    }
})
