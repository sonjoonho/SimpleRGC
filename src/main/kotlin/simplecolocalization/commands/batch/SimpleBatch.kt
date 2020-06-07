package simplecolocalization.commands.batch

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.io.Opener
import java.awt.Container
import java.awt.GridLayout
import java.io.File
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JSpinner
import javax.swing.JTabbedPane
import javax.swing.SpinnerNumberModel
import loci.formats.UnknownFormatException
import loci.plugins.BF
import loci.plugins.`in`.ImporterOptions
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import simplecolocalization.services.CellDiameterRange
import simplecolocalization.widgets.AlignedTextWidget

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

    /** Adds a spinner with the given label and model to the container, returning the JSpinner. */
    private fun addSpinner(container: Container, labelName: String, model: SpinnerNumberModel): JSpinner {
        val panel = JPanel()
        val label = JLabel(labelName)
        val spinner = JSpinner(model)
        label.labelFor = spinner
        panel.add(label)
        panel.add(spinner)
        container.add(panel)
        return spinner
    }

    /** Adds a checkbox with the given label to the container, returning the JCheckBox. */
    private fun addCheckBox(container: Container, labelName: String): JCheckBox {
        val panel = JPanel()
        val label = JLabel(labelName)
        val checkBox = JCheckBox()
        label.labelFor = checkBox
        panel.add(label)
        panel.add(checkBox)
        container.add(panel)
        return checkBox
    }

    /** Adds a file chooser with the given label to the container, returning the browseButton so an
     *  action listener can be added by the caller.
     */
    private fun addFileChooser(container: Container, labelName: String): JButton {
        val panel = JPanel()
        val label = JLabel(labelName)
        val browseButton = JButton("Browse")
        label.labelFor = browseButton
        panel.add(label)
        panel.add(browseButton)
        container.add(panel)

        return browseButton
    }

    private fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
        return if (shouldProcessFilesInNestedFolders) {
            file.walkTopDown().filter { f -> !f.isDirectory }.toList()
        } else {
            file.listFiles()?.filter { f -> !f.isDirectory }?.toList() ?: listOf(file)
        }
    }

    private fun openFiles(inputFiles: List<File>): List<ImagePlus> {
        /*
        First, we attempt to use the default ImageJ Opener. The ImageJ Opener falls back to a plugin called
        HandleExtraFileTypes when it cannot open a file - which attempts to use Bio-Formats when it encounters a LIF.
        Unfortunately, the LociImporter (what Bio-Formats uses) opens a dialog box when it does this. It does
        support a "windowless" option, but it's not possible to pass this option (or any of our desired options) through
        HandleExtraFileTypes. So instead, we limit the scope of possible file types by supporting native ImageJ formats
        (Opener.types), preventing HandleExtraFileTypes from being triggered, and failing this fall back to calling the
        Bio-Formats Importer manually. This handles the most common file types we expect to encounter.

        Also, note that Opener returns null when it fails to open a file, whereas the Bio-Formats Importer throws an
        UnknownFormatException`. To simplify the logic, an UnknownFormatException is thrown when Opener returns null.
        */
        val opener = Opener()
        val inputImages = mutableListOf<ImagePlus>()

        for (file in inputFiles) {

            try {
                if (Opener.types.contains(file.extension)) {
                    val image = opener.openImage(file.absolutePath) ?: throw UnknownFormatException()
                    inputImages.add(image)
                } else {
                    val options = ImporterOptions()
                    options.id = file.path
                    options.colorMode = ImporterOptions.COLOR_MODE_COMPOSITE
                    options.isAutoscale = true
                    options.setOpenAllSeries(true)

                    // Note that the call to BF.openImagePlus returns an array of images because a single LIF file can
                    // contain multiple series.
                    inputImages.addAll(BF.openImagePlus(options))
                }
            } catch (e: UnknownFormatException) {
                logService.warn("Skipping file with unsupported type \"${file.name}\"")
            } catch (e: NoClassDefFoundError) {
                MessageDialog(
                    IJ.getInstance(), "Error",
                    """
                    It appears that the Bio-Formats plugin is not installed.
                    Please enable the Fiji update site in order to enable this functionality.
                    """.trimIndent()
                )
            }
        }
        return inputImages
    }

    /** Creates the Simple Cell Counter GUI. */
    private fun simpleCellCounterPanel(): JPanel {
        // TODO: Make this pretty, add a layout manager
        // TODO: Make User parameters persist
        val panel = JPanel()
        panel.layout = GridLayout(0, 1)

        // TODO: Handle no directory chosen (when inputFolder is null)
        var inputFolder: File? = null
        val button = addFileChooser(panel, "Input folder")
        button.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            val i = fileChooser.showOpenDialog(panel)
            if (i == JFileChooser.APPROVE_OPTION) {
                inputFolder = fileChooser.selectedFile
            }
        }

        val shouldProcessFilesInNestedFoldersCheckbox = addCheckBox(panel, "Batch process in nested sub-folders?")

        val channelModel = SpinnerNumberModel(1, 0, 10, 1)
        val channelSpinner = addSpinner(panel, "Select channel to use", channelModel)

        val imageProcessingParametersLabel = JLabel("Image processing parameters")
        panel.add(imageProcessingParametersLabel)

        // TODO: Hook this up so it actually works
        val cellDiameterLabel = JLabel("Cell diameter(px)")
        val cellDiameterWidget = AlignedTextWidget()
        panel.add(cellDiameterLabel)

        val thresholdRadiusModel = SpinnerNumberModel(20, 1, 1000, 1)
        val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

        val gaussianBlurModel = SpinnerNumberModel(3, 1, 50, 1)
        val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)

        // TODO: Make removeAxons actually do something
        val removeAxonsCheckbox = addCheckBox(panel, "Remove Axons")

        val outputParamsLabel = JLabel("Output parameters")
        panel.add(outputParamsLabel)

        val resultsOutputPanel = JPanel()
        val resultsOutputLabel = JLabel("Results output")
        val saveAsCSVButton = JRadioButton("Save as a CSV file")
        val saveAsXMLButton = JRadioButton("Save as XML file")
        val bg = ButtonGroup()
        bg.add(saveAsCSVButton); bg.add(saveAsXMLButton)
        resultsOutputPanel.add(resultsOutputLabel)
        resultsOutputPanel.add(saveAsCSVButton)
        resultsOutputPanel.add(saveAsXMLButton)
        panel.add(resultsOutputPanel)

        // TODO: Handle no file selected (when outputFile is null)
        var outputFile: File? = null
        val browseButton = addFileChooser(panel, "Output File (if saving)")
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val i = fileChooser.showOpenDialog(panel)
            if (i == JFileChooser.APPROVE_OPTION) {
                outputFile = fileChooser.selectedFile
            }
        }

        val okButton = JButton("Ok")
        panel.add(okButton)
        okButton.addActionListener {
            val channel = channelSpinner.value as Int
            val thresholdRadius = thresholdRadiusSpinner.value as Int
            val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
            // val removeAxons = removeAxonsCheckbox.isSelected
            val outputFormat = when {
                saveAsCSVButton.isSelected -> OutputFormat.CSV
                saveAsXMLButton.isSelected -> OutputFormat.XML
                else -> // TODO: Handle no output format selected
                    ""
            }

            val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected

            // TODO: handle the null inputFolder correctly
            val files = getAllFiles(inputFolder!!, shouldProcessFilesInNestedFolders)

            val cellCounter = BatchableCellCounter(channel, context)

            // TODO: Use the user input cell diameter range
            cellCounter.process(openFiles(files), CellDiameterRange(0.0, 100.0), thresholdRadius, gaussianBlurSigma, outputFormat, outputFile!!)
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The batch processing results have successfully been saved to the specified file."
            )
        }

        return panel
    }

    /** Creates the Simple Colocalizer GUI. */
    private fun simpleColocalizerPanel(): JPanel {
        // TODO: Create panel for Simple Colocalizer
        return JPanel()
    }

    private fun gui() {
        val frame = JFrame()
        val simpleCellCounterPanel = simpleCellCounterPanel()
        val simpleColocalizerPanel = simpleColocalizerPanel()
        val tp = JTabbedPane()
        tp.setBounds(10, 10, 700, 700)
        tp.add("Simple Cell Counter", simpleCellCounterPanel)
        tp.add("Simple Colocalizer", simpleColocalizerPanel)
        frame.add(tp)
        frame.setSize(800, 800)

        frame.layout = null
        frame.isVisible = true
    }

    override fun run() {
        gui()
    }
}
