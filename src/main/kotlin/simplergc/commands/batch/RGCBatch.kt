package simplergc.commands.batch

import java.util.prefs.Preferences
import javax.swing.JFrame
import javax.swing.JTabbedPane
import net.imagej.ImageJ
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import simplergc.commands.batch.controllers.RGCCounterController
import simplergc.commands.batch.controllers.RGCTransductionController
import simplergc.commands.batch.models.RGCCounterModel
import simplergc.commands.batch.models.RGCTransductionModel
import simplergc.commands.batch.views.RGCCounterView
import simplergc.commands.batch.views.RGCTransductionView
import simplergc.services.CellSegmentationService

@Plugin(type = Command::class, menuPath = "Plugins > Simple RGC > RGC Batch")
class RGCBatch : Command {

    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var context: Context

    private val prefs = Preferences.userRoot().node(this.javaClass.name)

    object OutputFormat {
        const val CSV = "Save as CSV file"
    }

    private fun gui() {
        val frame = JFrame()

        val counterModel = RGCCounterModel(context, prefs)
        val counterView = RGCCounterView(counterModel)
        RGCCounterController(counterView, counterModel)

        val transductionModel = RGCTransductionModel(context, prefs)
        val transductionView = RGCTransductionView(transductionModel)
        RGCTransductionController(transductionView, transductionModel)

        val tp = JTabbedPane()
        tp.setBounds(10, 10, 500, 550)
        tp.add("RGCCounter", counterView)
        tp.add("RGCTransduction", transductionView)
        frame.add(tp)
        frame.setSize(525, 600)
        frame.isResizable = false

        frame.layout = null
        frame.isVisible = true
    }

    override fun run() {
        gui()
    }

    companion object {
        /**
         * Entry point to directly open the plugin, used for debugging purposes.
         *
         * @throws Exception
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val ij = ImageJ()

            ij.context().inject(CellSegmentationService())
            ij.launch()

            ij.command().run(RGCBatch::class.java, true)
        }
    }
}
