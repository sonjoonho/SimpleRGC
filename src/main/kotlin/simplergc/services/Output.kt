package simplergc.services

/**
 * Output defines an object that can process and output a Table.
 */
interface Output {

    val tableWriter: TableWriter

    companion object {
        const val ARTICLE_CITATION = "https://arxiv.org/abs/2008.06276"
        const val PLUGIN_VERSION = "1.1.0"
    }

    fun output()
}

/**
 * Parameters is a data structure that stores input parameters for writing.
 */
sealed class Parameters {
    data class Counter(
        val targetChannel: Int,
        val cellDiameterRange: CellDiameterRange,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double
    ) : Parameters()

    data class Transduction(
        val shouldRemoveAxonsFromTargetChannel: Boolean,
        val transducedChannel: Int,
        val shouldRemoveAxonsFromTransductionChannel: Boolean,
        val cellDiameterRange: CellDiameterRange,
        val localThresholdRadius: Int,
        val gaussianBlurSigma: Double,
        val targetChannel: Int
    ) : Parameters()
}
