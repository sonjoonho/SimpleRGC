package simplergc.services.counter.output

import org.scijava.ui.UIService

class ImageJTableCounterOutput(private val uiService: UIService) : CounterOutput() {

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        for ((fileName, count) in fileNameAndCountList) {
            resultsData.addRow(ResultsRow(fileName.replace(",", ""), count))
        }
        resultsData.produceImageJTable(uiService)
    }
}
