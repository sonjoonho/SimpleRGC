package simplecolocalization.services.colocalizer

import ij.gui.PolygonRoi
import ij.gui.Roi
import io.kotlintest.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class PositionedCellTest : StringSpec ({
    "fromRoi and toRoi should be idempotent" {
        val roi = PolygonRoi(floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f), PolygonRoi.POLYGON)
        val cell = PositionedCell.fromRoi(roi)
        val backToRoi =  cell.toRoi()
        // TODO(willburr): Check subset
    }
})