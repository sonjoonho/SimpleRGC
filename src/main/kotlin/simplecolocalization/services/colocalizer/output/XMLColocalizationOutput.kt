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
import simplecolocalization.services.CellColocalizationService

/**
 * Displays a table for a transduction analysis with the result of
 * overlapping, transduced cells.
 */
class XMLColocalizationOutput(
    private val analysis: Array<CellColocalizationService.CellAnalysis>,
    private val outputFile: File
) : ColocalizationOutput() {

    override fun output() {
        val doc = createXML()
        writeXML(doc)
    }

    /**
     *  Creates XML doc with the schema:
     *  <colocalizationresult file=inputfilename>
     *      <colocalizedcell>
     *          <area></area>
     *          <median></median>
     *          <mean></mean>
     *      </colocalizedcell>
     *      ...
     *      <colocalizedcell>...</>
     *  </colocalizationresult>
     */
    private fun createXML(): Document? {
        // Create XML factory, builder and document.
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.newDocument()

        val rootElement = doc.createElement("colocalizationresult")
        doc.appendChild(rootElement)

        // TODO: (tiger-cross) find way to get input filename.
        // // Create attribute for file
        // val fileAttr = doc.createAttribute("file")
        // fileAttr.value = file.name
        // rootElement.setAttributeNode(fileAttr)

        analysis.forEach {
            // Create a <colocalisedcell> element for each cell detected in both channels.
            val colocalizedCell = doc.createElement("colocalizedcell")
            rootElement.appendChild(colocalizedCell)

            addColocalizedCellAttribute("area", it.area.toString(), colocalizedCell, doc)
            addColocalizedCellAttribute("median", it.median.toString(), colocalizedCell, doc)
            addColocalizedCellAttribute("mean", it.mean.toString(), colocalizedCell, doc)
        }

        return doc
    }

    /**
     * Create a <attrName> element with the value as a Text Node for each colocalized cell.
     * Current possible attrName values are "area", "median" and "mean".
     */
    private fun addColocalizedCellAttribute(attrName: String, attrVal: String, parent: Element, doc: Document) {
        val elem = doc.createElement(attrName)
        parent.appendChild(elem)
        elem.appendChild(doc.createTextNode(attrVal))
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
        tr.transform(DOMSource(doc), StreamResult(FileOutputStream(outputFile)))
    }
}
