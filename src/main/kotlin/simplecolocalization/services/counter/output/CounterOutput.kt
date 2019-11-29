package simplecolocalization.services.counter.output

/**
 * Outputs the result of cell counting.
 */
abstract class CounterOutput {
    abstract fun addCountForFile(count: Int, file: String)
}
