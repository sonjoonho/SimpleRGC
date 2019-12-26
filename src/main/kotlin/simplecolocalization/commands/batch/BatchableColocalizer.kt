package simplecolocalization.commands.batch

import de.siegmar.fastcsv.writer.CsvWriter
import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.plugin.ChannelSplitter
import ij.plugin.ZProjector
import java.io.File
import java.nio.charset.StandardCharsets
import org.scijava.Context
import simplecolocalization.commands.SimpleColocalization

class BatchableColocalizer(private val targetChannel: Int, private val transducedChannel: Int, private val context: Context) : Batchable {
    override fun process(inputFiles: List<File>, outputFile: File) {
        val simpleColocalization = SimpleColocalization()
        context.inject(simpleColocalization)

        val analyses = inputFiles.mapNotNull {
            var image = ImagePlus(it.absolutePath)
            if (image.nSlices > 1) {
                // Flatten slices of the image. This step should probably be done during the preprocessing step - however
                // this operation is not done in-place but creates a new image, which makes this hard.
                image = ZProjector.run(image, "max")
            }

            val imageChannels = ChannelSplitter.split(image)
            if (targetChannel < 1 || targetChannel > imageChannels.size) {
                MessageDialog(
                    IJ.getInstance(),
                    "Error",
                    "Target channel $targetChannel does not exist in ${image.fileInfo.fileName}. There are ${imageChannels.size} channels available."
                )
                return@mapNotNull null
            }

            if (transducedChannel < 1 || transducedChannel > imageChannels.size) {
                MessageDialog(
                    IJ.getInstance(),
                    "Error",
                    "Transduced channel $transducedChannel does not exist in ${image.fileInfo.fileName}. There are ${imageChannels.size} channels available."
                )
                return@mapNotNull null
            }

            return@mapNotNull simpleColocalization.analyseColocalization(imageChannels[targetChannel], imageChannels[transducedChannel])
        }

        val fileNameAndAnalysis = inputFiles.map { it.name }.zip(analyses)
        val csvWriter = CsvWriter()
        val outputData = mutableListOf(arrayOf("File Name", "Total Target Cells", "Total Transduced Target Cells"))
        outputData.addAll(fileNameAndAnalysis.map {
            val totalTargetCells = it.second.targetCellCount.toString()
            val totalTransducedTargetCells = it.second.partitionedCells.overlapping.size.toString()
            arrayOf(it.first, totalTargetCells, totalTransducedTargetCells)
        })
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
