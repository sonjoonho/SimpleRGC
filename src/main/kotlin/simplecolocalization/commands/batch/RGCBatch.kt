package simplecolocalization.commands.batch

import javax.swing.JFrame
import javax.swing.JTabbedPane
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import simplecolocalization.commands.batch.views.rgcCounterPanel
import simplecolocalization.commands.batch.views.rgcTransductionPanel

@Plugin(type = Command::class, menuPath = "Plugins > Simple RGC > RGC Batch")
class RGCBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var context: Context

    object OutputFormat {
        const val CSV = "Save as CSV file"
        const val XML = "Save as XML file"
    }

    private fun gui() {
        val frame = JFrame()
        val simpleCellCounterPanel = rgcCounterPanel(context)
        val simpleColocalizerPanel = rgcTransductionPanel(context)
        val tp = JTabbedPane()
        tp.setBounds(5, 5, 500, 575)
        tp.add("RGCCounter", simpleCellCounterPanel)
        tp.add("RGCTransduction", simpleColocalizerPanel)
        frame.add(tp)
        frame.setSize(525, 625)
        frame.isResizable = false

        frame.layout = null
        frame.isVisible = true
    }

    override fun run() {
        gui()
    }
}
