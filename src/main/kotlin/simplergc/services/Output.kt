package simplergc.services

/**
 * Output defines an object that can process and output a Table.
 */
interface Output {

    val tableWriter: TableWriter

    companion object {
        const val ARTICLE_CITATION = "Cross T, Navarange R, Son J-H, Burr W, Singh A, Zhang K, Rusu M, Gkoutzis K, Osborne A, Nieuwenhuis B 2021 Simple RGC: ImageJ Plugins for Counting Retinal Ganglion Cells and Determining the Transduction Efficiency of Viral Vectors in Retinal Wholemounts. Journal of Open Research Software, 9: 15. DOI: https://doi.org/10.5334/jors.342"
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
