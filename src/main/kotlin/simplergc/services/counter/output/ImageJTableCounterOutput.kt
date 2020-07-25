package simplergc.services.counter.output

import org.scijava.ui.UIService
import simplergc.services.ImageJTableWriter

class ImageJTableCounterOutput(uiService: UIService) : CounterOutput() {

    override val tableWriter = ImageJTableWriter(uiService)

    /**
     * Displays GUI window using an ImageJ table to output count results.
     */
    override fun output() {
        for ((fileName, count) in fileNameAndCountList) {
            resultsData.addRow(ResultsRow(fileName.replace(",", ""), count))
        }
        tableWriter.produce(resultsData, "")
    }
}
