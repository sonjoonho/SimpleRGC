package simplecolocalization.services.colocalizer.output

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import simplecolocalization.commands.SimpleColocalization.TransductionResult
import simplecolocalization.services.SimpleOutput

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XMLColocalizationOutput(
    private val result: TransductionResult,
    private val file: File
) : SimpleOutput() {

    override fun output() {
        val doc = createXML()
        writeXML(doc)
    }

    /**
     *  Creates XML doc with the schema:
     *  <ColocalizationResult>
     *      <Summary>
     *          <TotalTargetCellCount></TotalTargetCellCount>
     *          <NumTransducedCellsOverlappingTarget></NumTransducedCellsOverlappingTarget>
     *          <NumCellsOverlappingThreeChannels></NumCellsOverlappingThreeChannels>
     *      </Summary>
     *      <ColocalizedCell>
     *          <Area></Area>
     *          <Median></Median>
     *          <Mean></Mean>
     *      </ColocalizedCell>
     *      ...
     *      <ColocalizedCell>
     *          ...
     *      </ColocalizedCell>
     *  </ColocalizationResult>
     */
    private fun createXML(): Document? {
        // Create XML factory, builder and document.
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.newDocument()

        val rootElement = doc.createElement("ColocalizationResult")
        doc.appendChild(rootElement)

        addSummary(doc, rootElement)

        result.overlappingTransducedIntensityAnalysis.forEach {
            // Create a <ColocalizedCell> element for each cell detected in both channels.
            val colocalizedCell = doc.createElement("ColocalizedCell")
            rootElement.appendChild(colocalizedCell)

            addAttribute("Area", it.area.toString(), colocalizedCell, doc)
            addAttribute("Median", it.median.toString(), colocalizedCell, doc)
            addAttribute("Mean", it.mean.toString(), colocalizedCell, doc)
            addAttribute("IntegratedDensity", (it.mean * it.area).toString(), colocalizedCell, doc)
            addAttribute("RawIntegratedDensity", it.sum.toString(), colocalizedCell, doc)
        }

        return doc
    }

    /**
     * Create a <attrName> element with the value as a Text Node for a given parent element.
     * Current possible attrName values are "Area", "Median" and "Mean" for a colocalisaed cell.
     * Also used for creating the summary attributes.
     */
    private fun addAttribute(attrName: String, attrVal: String, parent: Element, doc: Document) {
        val elem = doc.createElement(attrName)
        parent.appendChild(elem)
        elem.appendChild(doc.createTextNode(attrVal))
    }

    /**
     * Adds a summary section to the XML output.
     */
    private fun addSummary(doc: Document, root: Element) {
        val summary = doc.createElement("Summary")
        root.appendChild(summary)
        addAttribute("NumCellsInCellMorphology1", result.targetCellCount.toString(), summary, doc)
        addAttribute(
            "TransducedCellsInChannel1",
            result.overlappingTwoChannelCells.size.toString(),
            summary,
            doc
        )
        if (result.overlappingThreeChannelCells != null) {
            addAttribute(
                "TransducedCellsInBothMorphologyChannels",
                result.overlappingThreeChannelCells.size.toString(),
                summary,
                doc
            )
        }

        val transductionEfficiency = (result.overlappingTwoChannelCells.size / result.targetCellCount.toDouble()) * 100
        addAttribute("TransductionEfficiency (%)", transductionEfficiency.toString(), summary, doc)

        addAttribute("MeanIntensityOfColocalizedCells",
            (result.overlappingTransducedIntensityAnalysis.sumBy { it.mean } / result.overlappingTransducedIntensityAnalysis.size).toString(),
            summary,
            doc
        )
    }

    @Throws(TransformerException::class, IOException::class)
    private fun writeXML(doc: Document?) {
        // Create transformer and set output properties.
        val tr = TransformerFactory.newInstance().newTransformer()
        tr.setOutputProperty(OutputKeys.INDENT, "yes")
        tr.setOutputProperty(OutputKeys.METHOD, "xml")
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd")
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        // Send output file to file output stream.
        tr.transform(DOMSource(doc), StreamResult(FileOutputStream(file)))
    }
}
