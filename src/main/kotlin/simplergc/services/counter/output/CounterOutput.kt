package simplergc.services.counter.output

import simplergc.services.SimpleOutput

/**
 * Outputs the result of cell counting.
 */
abstract class CounterOutput : SimpleOutput() {
    abstract fun addCountForFile(count: Int, file: String)
}
