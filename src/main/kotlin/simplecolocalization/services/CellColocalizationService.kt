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

    // TODO(sonjoonho): Better documentation needed.
    /** Analyses the channel intensity of the cells. */
    fun analyseCells(image: ImagePlus, highlightedCells: Array<Roi>): Array<CellSegmentationService.CellAnalysis> {
        // Split the image into multiple grayscale images (one for each channel).
        val channelImages = ChannelSplitter.split(image)
        val numberOfChannels = channelImages.size

        val analyses = arrayListOf<CellSegmentationService.CellAnalysis>()
        for (cell in highlightedCells) {
            var area = 0
            val sums = MutableList(numberOfChannels) { 0 }
            val mins = MutableList(numberOfChannels) { Integer.MAX_VALUE }
            val maxs = MutableList(numberOfChannels) { Integer.MIN_VALUE }
            val containedCells = cell.containedPoints
            containedCells.forEach { point ->
                area++
                for (channel in 0 until numberOfChannels) {
                    // pixelData is of the form [value, 0, 0, 0] because ImageJ.
                    val pixelData = channelImages[channel].getPixel(point.x, point.y)
                    sums[channel] += pixelData[0]
                    mins[channel] = Integer.min(mins[channel], pixelData[0])
                    maxs[channel] = Integer.max(maxs[channel], pixelData[0])
                }
            }
            val channels = mutableListOf<CellSegmentationService.ChannelAnalysis>()
            for (channel in 0 until numberOfChannels) {
                channels.add(
                    CellSegmentationService.ChannelAnalysis(
                        channelImages[channel].title,
                        sums[channel] / area,
                        mins[channel],
                        maxs[channel]
                    )
                )
            }
            analyses.add(CellSegmentationService.CellAnalysis(area, channels))
        }

        return analyses.toTypedArray()
    }

    /** Counts the number of analysed cells that exceed the channel intensity threshold. */
    fun countChannel(analyses: Array<CellSegmentationService.CellAnalysis>, channel: Int, threshold: Double): Int {
        return analyses.count { it.channels[channel].mean > threshold }
    }

    fun markOverlappingCells(originalImage: ImagePlus, roiManager: RoiManager, overlapping: List<Roi>) {
        overlapping.forEach { r ->
            roiManager.addRoi(r)
            r.image = originalImage
        }
    }
}
