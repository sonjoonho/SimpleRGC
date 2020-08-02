package simplergc.services

enum class Metric(
    val value: String,
    val full: String,
    val description: String,
    val compute: (CellColocalizationService.CellAnalysis) -> Int,
    val channels: ChannelSelection,
    val summaryName: String? = null
) {
    Area(
        "Morphology Area",
        "Morphology Area (pixel^2)",
        "Average morphology area (pixel²) for each transduced cell",
        CellColocalizationService.CellAnalysis::area,
        ChannelSelection.TRANSDUCTION_ONLY,
        "Average Morphology Area"
    ),
    Mean(
        "Mean Int",
        "Mean Fluorescence Intensity (a.u.)",
        "Mean fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::mean,
        ChannelSelection.ALL_CHANNELS
    ),
    Median(
        "Median Int",
        "Median Fluorescence Intensity (a.u.)",
        "Median fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::median,
        ChannelSelection.ALL_CHANNELS
    ),
    Min(
        "Min Int",
        "Min Fluorescence Intensity (a.u.)",
        "Min fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::min,
        ChannelSelection.ALL_CHANNELS
    ),
    Max(
        "Max Int",
        "Max Fluorescence Intensity (a.u.)",
        "Max fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::max,
        ChannelSelection.ALL_CHANNELS
    ),
    IntDen(
        "Raw IntDen",
        "Raw Integrated Density",
        "Raw Integrated Density for each transduced cell",
        CellColocalizationService.CellAnalysis::rawIntDen,
        ChannelSelection.ALL_CHANNELS
    );

    enum class ChannelSelection {
        TRANSDUCTION_ONLY,
        ALL_CHANNELS
    }
}
