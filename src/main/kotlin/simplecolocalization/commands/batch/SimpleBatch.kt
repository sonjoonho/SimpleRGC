package simplecolocalization.commands.batch

import javax.swing.JFrame
import javax.swing.JTabbedPane
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import simplecolocalization.commands.batch.views.simpleCellCounterPanel
import simplecolocalization.commands.batch.views.simpleColocalizerPanel

@Plugin(type = Command::class, menuPath = "Plugins > Simple Cells > Simple Batch")
class SimpleBatch : Command {

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
        val simpleCellCounterPanel = simpleCellCounterPanel(context)
        val simpleColocalizerPanel = simpleColocalizerPanel(context)
        val tp = JTabbedPane()
        tp.setBounds(5, 5, 500, 575)
        tp.add("Simple Cell Counter", simpleCellCounterPanel)
        tp.add("Simple Colocalizer", simpleColocalizerPanel)
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
