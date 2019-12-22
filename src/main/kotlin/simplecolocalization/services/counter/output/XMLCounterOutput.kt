package simplecolocalization.services.counter.output

import org.w3c.dom.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XMLCounterOutput(private val outputFile: File) : CounterOutput() {

    private val fileNameAndCountList: ArrayList<Pair<String, Int>> = ArrayList()

    override fun addCountForFile(count: Int, file: String) {
        fileNameAndCountList.add(Pair(file, count))
    }

    /***
     *  Creates XML doc with the schema:
     *  <counts>
     *      <count file=inputfilename>countvalue</count>
     *      ...
     *      <count file=inputfilename>countvalue</count>
     *  </counts>
     */
    private fun createXML(): Document? {
        // Create xml factory, builder and document
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.newDocument()

        // root <counts> element
        val rootElement = doc.createElement("counts")
        doc.appendChild(rootElement)

        fileNameAndCountList.forEach {
            // Create a <count> element for each file counted
            val count = doc.createElement("count")
            rootElement.appendChild(count)

            // Create attribute for file
            val fileAttr = doc.createAttribute("file")
            fileAttr.setValue(it.first)
            count.setAttributeNode(fileAttr)

            // Add count as value inside count element
            count.appendChild(doc.createTextNode(it.second.toString()))
        }

        // TODO: tiger-cross, handle any exceptions here?
        return doc
    }

    private fun writeXML(doc: Document?) {
        try {
            // Create transformer and set output properties
            val tr = TransformerFactory.newInstance().newTransformer()
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty(OutputKeys.METHOD, "xml")
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            // Send output file to file output stream.
            tr.transform(DOMSource(doc), StreamResult(FileOutputStream(outputFile)))
        } catch (te: TransformerException) {
            // TODO: tiger-cross, add error dialogues here.
            println(te.message)
        } catch (ioe: IOException) {
            println(ioe.message)
        }
    }

    fun save() {
        val doc = createXML()

        writeXML(doc)
    }
}