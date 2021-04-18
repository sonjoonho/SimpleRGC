package simplergc.services

const val UTF_8_SUP2 = "\u00b2"

/**
 * A metric that is evaluated for each cell.
 *
 * @param value: Shortened name for Metric, used in sheets/filenames.
 * @param full: Full-length name for Metric, used in headers of tables.
 * @param description: Description of metric, used in Documentation tables.
 * @param compute: Function to be used to compute value of metric for a cell.
 * @param channels: Determines set of channels to compute metric for, either TRANSDUCTION_ONLY or ALL_CHANNELS.
 * @param summaryName: Optional summary name to indicate name to be used in Summary table (defaults to value of [full])
 */
enum class Metric(
    val value: String,
    val full: String,
    val description: String,
    val compute: (CellColocalizationService.CellAnalysis) -> Number,
    val channels: ChannelSelection,
    val toField: (Number) -> Field<*>,
    summaryName: String? = null
) {
    Area(
        "Morphology Area",
        "Morphology Area (pixel$UTF_8_SUP2)",
        "Average morphology area (pixel$UTF_8_SUP2) for each transduced cell",
        CellColocalizationService.CellAnalysis::area,
        ChannelSelection.TRANSDUCTION_ONLY,
        { IntField(it.toInt()) },
        "Average Morphology Area (pixel$UTF_8_SUP2)"
    ),
    Mean(
        "Mean Int",
        "Mean Fluorescence Intensity (a.u.)",
        "Mean fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::mean,
        ChannelSelection.ALL_CHANNELS,
        { DoubleField(it.toDouble()) }
    ),
    Median(
        "Median Int",
        "Median Fluorescence Intensity (a.u.)",
        "Median fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::median,
        ChannelSelection.ALL_CHANNELS,
        { IntField(it.toInt()) }
    ),
    Min(
        "Min Int",
        "Min Fluorescence Intensity (a.u.)",
        "Min fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::min,
        ChannelSelection.ALL_CHANNELS,
        { IntField(it.toInt()) }
    ),
    Max(
        "Max Int",
        "Max Fluorescence Intensity (a.u.)",
        "Max fluorescence intensity for each transduced cell",
        CellColocalizationService.CellAnalysis::max,
        ChannelSelection.ALL_CHANNELS,
        { IntField(it.toInt()) }
    ),
    IntDen(
        "Raw IntDen",
        "Raw Integrated Density",
        "Raw Integrated Density for each transduced cell",
        CellColocalizationService.CellAnalysis::rawIntDen,
        ChannelSelection.ALL_CHANNELS,
        { IntField(it.toInt()) }
    );

    val summaryHeader = summaryName ?: full

    enum class ChannelSelection {
        TRANSDUCTION_ONLY,
        ALL_CHANNELS
    }
}
