package simplergc.services.counter.output

import simplergc.services.SimpleOutput

/**
 * Outputs the result of cell counting.
 */
interface CounterOutput : SimpleOutput {

    companion object {
        const val PLUGIN_NAME = "RGC Counter"
    }

    fun addCountForFile(count: Int, file: String)
}
