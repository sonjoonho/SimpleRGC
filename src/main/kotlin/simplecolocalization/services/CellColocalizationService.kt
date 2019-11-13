package simplecolocalization.services

import ij.ImagePlus
import ij.gui.Roi
import ij.plugin.ChannelSplitter
import ij.plugin.frame.RoiManager
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service

@Plugin(type = Service::class)
class CellColocalizationService : AbstractService(), ImageJService {

    data class CellAnalysis(val area: Int, val mean: Int, val min: Int, val max: Int)

    /**
     *  Analyses the intensity of a cell.
     *
     *  Takes in [image], a greyscale image (representing the target channel).
     */
    fun analyseCellIntensity(image: ImagePlus, cells: Array<Roi>): Array<CellAnalysis> {
        return cells.map { cell ->
            var area = 0
            var sum = 0
            var min = Integer.MAX_VALUE
            var max = Integer.MIN_VALUE
            cell.containedPoints.forEach { point ->
                // pixelData is of the form [value, 0, 0, 0] because ImageJ.
                val pixelData = image.getPixel(point.x, point.y)
                area++
                sum += pixelData[0]
                min = Integer.min(min, pixelData[0])
                max = Integer.max(max, pixelData[0])
            }
            CellAnalysis(area, sum / area, min, max)
        }.toTypedArray()
    }

    /** Counts the number of analysed cells that exceed the channel intensity threshold. */
    fun countChannel(analyses: Array<CellAnalysis>, threshold: Double): Int {
        return analyses.count { it.mean > threshold }
    }

    fun markOverlappingCells(originalImage: ImagePlus, roiManager: RoiManager, overlapping: List<Roi>) {
        overlapping.forEach { r ->
            roiManager.addRoi(r)
            r.image = originalImage
        }
    }
}
