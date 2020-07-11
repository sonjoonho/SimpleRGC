package simplergc.services.colocalizer.output

import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Outputs the result of cell counting.
 */
interface ColocalizationOutput : SimpleOutput {

    val fileNameAndResultsList: ArrayList<Pair<String, TransductionResult>>
        get() = ArrayList()

    companion object {
        const val PLUGIN_NAME = "RGC Transduction"
    }

    fun addTransductionResultForFile(transductionResult: TransductionResult, file: String) {
        fileNameAndResultsList.add(Pair(file, transductionResult))
    }
}
