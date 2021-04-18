package simplergc.services.counter.output

import org.scijava.ui.UIService
import simplergc.services.Aggregate
import simplergc.services.AggregateRow
import simplergc.services.FieldRow
import simplergc.services.HeaderField
import simplergc.services.ImageJTableWriter

class ImageJTableCounterOutput(uiService: UIService) : CounterOutput() {

    override val tableWriter = ImageJTableWriter(uiService)

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        resultsData.addRow(FieldRow(listOf("File Name", "Cell Count").map { HeaderField(it) }))
        for ((fileName, count) in fileNameAndCountList) {
            resultsData.addRow(ResultsRow(fileName.replace(",", ""), count))
        }
        tableWriter.produce(resultsData, "")
    }

    override fun generateAggregateRow(
        aggregate: Aggregate,
        cellCounts: List<Number>,
        spaces: Int,
        startRow: Int
    ): AggregateRow {
        TODO("No-op. This function should only be called in RGC-Batch.")
    }
}
