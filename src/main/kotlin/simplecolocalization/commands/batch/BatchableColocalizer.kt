package simplecolocalization.commands.batch

import de.siegmar.fastcsv.writer.CsvWriter
import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import java.io.File
import java.nio.charset.StandardCharsets
import org.scijava.Context
import simplecolocalization.commands.ChannelDoesNotExistException
import simplecolocalization.commands.SimpleColocalization
import simplecolocalization.preprocessing.PreprocessingParameters

class BatchableColocalizer(private val targetChannel: Int, private val transducedChannel: Int, private val context: Context) : Batchable {
    override fun process(inputImages: List<ImagePlus>, outputFile: File, preprocessingParameters: PreprocessingParameters) {
        val simpleColocalization = SimpleColocalization()

        // TODO(sonjoonho): I hate this
        simpleColocalization.targetChannel = targetChannel
        simpleColocalization.transducedChannel = transducedChannel
        context.inject(simpleColocalization)

        val analyses = inputImages.mapNotNull {
            try {
                simpleColocalization.process(it)
            } catch (e: ChannelDoesNotExistException) {
                MessageDialog(IJ.getInstance(), "Error", e.message)
                null
            }
        }

        val fileNameAndAnalysis = inputImages.map { it.title }.zip(analyses)
        val csvWriter = CsvWriter()
        val outputData = mutableListOf(arrayOf("File Name", "Total Target Cells", "Total Transduced Target Cells", ""))
        outputData.addAll(fileNameAndAnalysis.map {
            val totalTargetCells = it.second.targetCellCount.toString()
            val totalTransducedTargetCells = it.second.overlappingTwoChannelCells.size.toString()
            arrayOf(it.first, totalTargetCells, totalTransducedTargetCells)
        })
        csvWriter.write(outputFile, StandardCharsets.UTF_8, outputData)
    }
}
