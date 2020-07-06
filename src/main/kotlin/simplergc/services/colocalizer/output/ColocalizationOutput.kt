package simplergc.services.colocalizer.output

import simplergc.commands.RGCTransduction.TransductionResult
import simplergc.services.SimpleOutput

/**
 * Outputs the result of cell counting.
 */
abstract class ColocalizationOutput : SimpleOutput() {

    companion object {
        const val PLUGIN_NAME = "RGC Transduction"
        const val PLUGIN_VERSION = "1.0.0"
    }

    abstract fun addTransductionResultForFile(transductionResult: TransductionResult, file: String)
}
