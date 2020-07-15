package simplergc.services.colocalizer.output

import java.io.File
import simplergc.commands.RGCTransduction
import simplergc.services.BaseRow
import simplergc.services.CSV
import simplergc.services.CellColocalizationService
import simplergc.services.SimpleOutput.Companion.PLUGIN_VERSION
import simplergc.services.colocalizer.output.ColocalizationOutput.Companion.PLUGIN_NAME

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class CSVColocalizationOutput(
    outputFile: File,
    private val shouldRemoveAxonFromTargetChannel: Boolean,
    private val transducedChannel: Int,
    private val shouldRemoveAxonFromTransductionChannel: Boolean,
    private val cellDiameterText: String,
    private val localThresholdRadius: Int,
    private val gaussianBlurSigma: Double,
    private val targetChannel: Int
) : ColocalizationOutput {

    private val outputPath: String = "${outputFile.path}${File.separator}"

    private val documentationCsv = CSV(File("${outputPath}Documentation.csv"), arrayOf())
    data class DocumentationRow(val key: String, val description: String) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(key, description)
    }

    private val summaryCsv = CSV(File("${outputPath}Summary.csv"), arrayOf(
        "File Name",
        "Number of Cells",
        "Number of Transduced Cells",
        "Transduction Efficiency (%)",
        "Average Morphology Area (pixel^2)",
        "Mean Fluorescence Intensity (a.u.)",
        "Median Fluorescence Intensity (a.u.)",
        "Min Fluorescence Intensity (a.u.)",
        "Max Fluorescence Intensity (a.u.)",
        "RawIntDen"
    ))
    data class SummaryRow(
        val fileName: String,
        val summary: RGCTransduction.TransductionResult.Summary
    ) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(
            fileName,
            summary.targetCellCount.toString(),
            summary.transducedCellCount.toString(),
            summary.transductionEfficiency.toString(),
            summary.avgMorphologyArea.toString(),
            summary.meanFluorescenceIntensity.toString(),
            summary.medianFluorescenceIntensity.toString(),
            summary.minFluorescenceIntensity.toString(),
            summary.maxFluorescenceIntenstity.toString(),
            summary.rawIntDen.toString()
        )
    }

    override fun output() {

        writeDocumentationCsv()

        writeSummaryCsv()

        writeTransductionAnalysisCsv()

        writeParametersCsv()
    }

    private fun writeDocumentationCsv() {
        // Constant array of information
        documentationCsv.addRow(DocumentationRow("The Article: ", "TODO: insert full citation of manuscript when complete"))
        documentationCsv.addRow(DocumentationRow("", ""))
        documentationCsv.addRow(DocumentationRow("Abbreviation: ", "Description"))
        documentationCsv.addRow(DocumentationRow("Summary: ", "Key overall measurements per image"))
        documentationCsv.addRow(DocumentationRow("Transduced Cell Analysis: ", "Cell-by-cell metrics of transduced cells"))
        documentationCsv.addRow(DocumentationRow("Parameters: ", "Parameters used for SimpleRGC plugin"))
        documentationCsv.produce()
    }

    private fun writeSummaryCsv() {
        // Summary
        // TODO (#156): Add integrated density
        fileNameAndResultsList.forEach {
            summaryCsv.addRow(SummaryRow(it.first, it.second.getSummary()))
        }
        summaryCsv.produce()
    }

    private val transductionAnalysisCsv = CSV(File("${outputPath}Transduced Cell Analysis.csv"), arrayOf(
        "File Name",
        "Transduced Cell",
        "Morphology Area (pixel^2)",
        "Mean Fluorescence Intensity (a.u.)",
        "Median Fluorescence Intensity (a.u.)",
        "Min Fluorescence Intensity (a.u.)",
        "Max Fluorescence Intensity (a.u.)",
        "RawIntDen"
    ))
    data class TransductionAnalysisRow(
        val fileName: String,
        val cellAnalysis: CellColocalizationService.CellAnalysis
    ) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(
            fileName,
            "1",
            cellAnalysis.area.toString(),
            cellAnalysis.mean.toString(),
            cellAnalysis.median.toString(),
            cellAnalysis.min.toString(),
            cellAnalysis.max.toString(),
            cellAnalysis.rawIntDen.toString()
        )
    }

    private fun writeTransductionAnalysisCsv() {
        fileNameAndResultsList.forEach {
            val fileName = it.first
            it.second.overlappingTransducedIntensityAnalysis.forEach { cellAnalysis ->
                transductionAnalysisCsv.addRow(TransductionAnalysisRow(fileName, cellAnalysis))
            }
        }
        transductionAnalysisCsv.produce()
    }

    private val parametersCSV = CSV(File("${outputPath}Parameters.csv"), arrayOf(
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
    ))
    data class ParametersRow(
        val fileName: String,
        val morphologyChannel: Int,
        val excludeAxonsFromMorphologyChannel: Boolean,
        val transductionChannel: Int,
        val excludeAxonsFromTransductionChannel: Boolean,
        val cellDiameterText: String,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : BaseRow {
        override fun toStringArray(): Array<String> = arrayOf(
            fileName,
            PLUGIN_NAME,
            PLUGIN_VERSION,
            morphologyChannel.toString(),
            excludeAxonsFromMorphologyChannel.toString(),
            transductionChannel.toString(),
            excludeAxonsFromTransductionChannel.toString(),
            cellDiameterText,
            localThresholdRadius.toString(),
            gaussianBlurSigma.toString()
        )
    }
    private fun writeParametersCsv() {
        // TODO (#156): Add pixel size (micrometers) in next sprint.
        fileNameAndResultsList.forEach {
            transductionAnalysisCsv.addRow(
                ParametersRow(
                    it.first,
                    targetChannel,
                    shouldRemoveAxonFromTargetChannel,
                    transducedChannel,
                    shouldRemoveAxonFromTransductionChannel,
                    cellDiameterText,
                    localThresholdRadius,
                    gaussianBlurSigma
                )
            )
        }
        transductionAnalysisCsv.produce()
    }
}
