package simplecolocalization.services.counter.output

import simplecolocalization.services.SimpleOutput

/**
 * Outputs the result of cell counting.
 */
abstract class CounterOutput : SimpleOutput() {
    abstract fun addCountForFile(count: Int, file: String)
}
