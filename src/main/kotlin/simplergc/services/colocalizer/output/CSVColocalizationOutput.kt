package simplergc.services.colocalizer.output

import de.siegmar.fastcsv.writer.CsvWriter
import simplergc.commands.RGCTransduction
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    private val transductionParameters: RGCTransduction.TransductionParameters,
    private val result: TransductionResult,
    private val outputFile: File
) : SimpleOutput() {

    override fun output() {
        val csvWriter = CsvWriter()
        val outputFileSuccess = File(outputFile.path).mkdir()
        if (!outputFileSuccess and !outputFile.exists()) {
            throw IOException()
        }

        val documentationData = ArrayList<Array<String>>()
        documentationData.add(arrayOf("The Article: ", "TODO: insert full citation of manuscript when complete"))
        documentationData.add(arrayOf("", ""))
        documentationData.add(arrayOf("Abbreviation: ", "Description"))
        documentationData.add(arrayOf("Summary: ", "Key overall measurements per image"))
        documentationData.add(arrayOf("Transduced Cell Analysis: ", "Cell-by-cell metrics of transduced cells"))
        documentationData.add(arrayOf("Parameters: ", "Parameters used for SimpleRGC plugin"))

        csvWriter.write(
            File("${outputFile.path}${File.separator}Documentation.csv"),
            StandardCharsets.UTF_8,
            documentationData
        )

        // Summary
        val summaryData = ArrayList<Array<String>>()
        summaryData.add(
            arrayOf(
                "File Name",
                "Number of Cells",
                "Number of Transduced Cells",
                "Transduction Efficiency (%)",
                "Average Morphology Area (pixel^2)",
                "Mean Fluorescence Intensity (a.u.)",
                "Median Fluorescence Intensity (a.u.)",
                "Min Fluorescence Intensity (a.u.)",
                "Max Fluorescence Intensity (a.u.)",
                "IntDen",
                "RawIntDen"
            )
        )
        // TODO (tiger-cross): Calculate other metrics
        summaryData.add(
            arrayOf(
                transductionParameters.inputFileName,
                result.targetCellCount.toString(),
                result.overlappingTwoChannelCells.size.toString(),
                ((result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100).toString(),
                (result.overlappingTransducedIntensityAnalysis.sumBy { it.area } / result.overlappingTransducedIntensityAnalysis.size).toString(),
                (result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size).toString(),
                (result.overlappingTransducedIntensityAnalysis.sumBy { it.median } / result.overlappingTransducedIntensityAnalysis.size).toString(),
                "TODO: Min",
                "TODO: Max",
                "TODO: IntDen",
                "TODO: RawIntDen"
            )
        )
        csvWriter.write(File("${outputFile.path}${File.separator}Summary.csv"), StandardCharsets.UTF_8, summaryData)

        // Per-cell analysis
        val cellByCellData = ArrayList<Array<String>>()
        cellByCellData.add(
            arrayOf(
                "File Name",
                "Transduced Cell",
                "Morphology Area (pixel^2)",
                "Mean Fluorescence Intensity (a.u.)",
                "Median Fluorescence Intensity (a.u.)",
                "Min Fluorescence Intensity (a.u.)",
                "Max Fluorescence Intensity (a.u.)",
                "IntDen",
                "RawIntDen"
            )
        )
        result.overlappingTransducedIntensityAnalysis.forEach {
            cellByCellData.add(
                arrayOf(
                    transductionParameters.inputFileName,
                    "1",
                    it.area.toString(),
                    it.mean.toString(),
                    it.median.toString(),
                    "TODO: Min",
                    "TODO: Max",
                    "TODO: IntDen",
                    "TODO: RawIntDen"
                )
            )
        }
        csvWriter.write(
            File("${outputFile.path}${File.separator}Transduced Cell Analysis.csv"),
            StandardCharsets.UTF_8,
            cellByCellData
        )

        // TODO: Add pixel size (micrometers) at a later date
        // Parameters
        val parametersData = ArrayList<Array<String>>()
        parametersData.add(
            arrayOf(
                "File Name",
                "SimpleRGC Plugin",
                "Plugin Version",
                "Morphology channel",
                "Exclude Axons from morphology channel?",
                "Transduction channel",
                "Exclude Axons from transduction channel?",
                "Cell diameter range (px)",
                "Local threshold radius",
                "Gaussian blur sigma"
            )
        )
        parametersData.add(
            arrayOf(
                transductionParameters.inputFileName,
                transductionParameters.pluginName,
                transductionParameters.pluginVersion,
                transductionParameters.morphologyChannel,
                transductionParameters.excludeAxonsFromMorphologyChannel,
                transductionParameters.transductionChannel,
                transductionParameters.excludeAxonsFromTransductionChannel,
                transductionParameters.cellDiameterRange,
                transductionParameters.localThresholdRadius,
                transductionParameters.gaussianBlurSigma
            )
        )

        csvWriter.write(
            File("${outputFile.path}${File.separator}Parameters.csv"),
            StandardCharsets.UTF_8,
            parametersData
        )
    }
}
