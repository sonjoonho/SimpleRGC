package simplecolocalization.services

import ij.ImagePlus
import ij.gui.Roi
import net.imagej.ImageJService
import org.scijava.plugin.Plugin
import org.scijava.service.AbstractService
import org.scijava.service.Service

@Plugin(type = Service::class)
class CellColocalizationService : AbstractService(), ImageJService {

    data class CellAnalysis(val area: Int, val mean: Int, val median: Int)

    /**
     *  Analyses the intensity of a cell.
     *
     *  Takes in [image], a greyscale image (representing the target channel).
     */
    fun analyseCellIntensity(image: ImagePlus, cells: Array<Roi>): Array<CellAnalysis> {
        return cells.map { cell ->
            var area = 0
            var sum = 0
            cell.containedPoints.forEach { point ->
                // pixelData is of the form [value, 0, 0, 0] because ImageJ.
                val pixelData = image.getPixel(point.x, point.y)
                area++
                sum += pixelData[0]
            }
            val median = cell.containedPoints.map{ image.getPixel(it.x, it.y)[0] }.sorted().let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2 }
            CellAnalysis(area, sum / area, median)
        }.toTypedArray()
    }

    /** Counts the number of analysed cells that exceed the channel intensity threshold. */
    fun countChannel(analyses: Array<CellAnalysis>, threshold: Double): Int {
        return analyses.count { it.mean > threshold }
    }
}
