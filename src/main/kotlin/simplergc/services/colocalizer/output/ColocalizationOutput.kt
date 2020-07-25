package simplergc.services.colocalizer.output

import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.BaseRow
import simplergc.services.BooleanField
import simplergc.services.CellColocalizationService
import simplergc.services.DoubleField
import simplergc.services.IntField
import simplergc.services.SimpleOutput
import simplergc.services.StringField
import simplergc.services.Table

/**
 * Outputs the result of cell counting.
 */
abstract class ColocalizationOutput : SimpleOutput {

    val fileNameAndResultsList = mutableListOf<Pair<String, TransductionResult>>()

    data class DocumentationRow(val key: String, val description: String) : BaseRow {
        override fun toList() = listOf(StringField(key), StringField(description))
    }

    companion object {
        const val PLUGIN_NAME = "RGC Transduction"
    }

    open fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
    }

    protected val summaryData = Table(
        listOf(
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
        val summary: TransductionResult.Summary
    ) : BaseRow {
        override fun toList() = listOf(
            StringField(fileName),
            IntField(summary.targetCellCount),
            IntField(summary.transducedCellCount),
            DoubleField(summary.transductionEfficiency),
            IntField(summary.avgMorphologyArea),
            IntField(summary.meanFluorescenceIntensity),
            IntField(summary.medianFluorescenceIntensity),
            IntField(summary.minFluorescenceIntensity),
            IntField(summary.maxFluorescenceIntenstity),
            IntField(summary.rawIntDen)
        )
    }

    protected val transductionAnalysisData = Table(
        listOf(
            "File Name",
            "Transduced Cell",
            "Morphology Area (pixel^2)",
            "Mean Fluorescence Intensity (a.u.)",
            "Median Fluorescence Intensity (a.u.)",
            "Min Fluorescence Intensity (a.u.)",
            "Max Fluorescence Intensity (a.u.)",
            "RawIntDen"
        )
    )

    data class TransductionAnalysisRow(
        val fileName: String,
        val transducedCell: Int,
        val cellAnalysis: CellColocalizationService.CellAnalysis
    ) : BaseRow {
        override fun toList() = listOf(
            StringField(fileName),
            IntField(transducedCell),
            IntField(cellAnalysis.area),
            IntField(cellAnalysis.mean),
            IntField(cellAnalysis.median),
            IntField(cellAnalysis.min),
            IntField(cellAnalysis.max),
            IntField(cellAnalysis.rawIntDen)
        )
    }

    protected val parametersData = Table(
        listOf(
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
        override fun toList() = listOf(
            StringField(fileName),
            StringField(PLUGIN_NAME),
            StringField(SimpleOutput.PLUGIN_VERSION),
            IntField(morphologyChannel),
            BooleanField(excludeAxonsFromMorphologyChannel),
            IntField(transductionChannel),
            BooleanField(excludeAxonsFromTransductionChannel),
            StringField(cellDiameterText),
            IntField(localThresholdRadius),
            DoubleField(gaussianBlurSigma)
        )
    }
}
