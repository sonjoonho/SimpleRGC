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

    /** Runs BatchableCellCounter, called in action listener for "Ok" button. */
    private fun runSimpleCellCounter(
        inputFolder: File?,
        shouldProcessFilesInNestedFolders: Boolean,
        channel: Int,
        thresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        outputFile: File?
    ) {
        if (inputFolder == null) {
            MessageDialog(
                IJ.getInstance(), "Error", "No input folder selected"
            )
        } else if (outputFile == null) {
            MessageDialog(
                IJ.getInstance(), "Error", "No output file selected"
            )
        } else if (!inputFolder.exists()) {
            MessageDialog(
                IJ.getInstance(), "Error",
                "The input folder could not be opened. Please create it if it does not already exist"
            )
        } else {
            val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)

            val cellCounter = BatchableCellCounter(channel, context)

            // TODO: Use the user input cell diameter range
            cellCounter.process(
                openFiles(files),
                CellDiameterRange(0.0, 100.0),
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                outputFile
            )
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The batch processing results have successfully been saved to the specified file."
            )
        }
    }

    /** Creates the Simple Cell Counter GUI. */
    private fun simpleCellCounterPanel(): JPanel {
        // TODO: Make this pretty
        // TODO: Make User parameters persist
        val panel = JPanel()
        panel.layout = GridLayout(0, 1)

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
            val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
            val channel = channelSpinner.value as Int
            val thresholdRadius = thresholdRadiusSpinner.value as Int
            val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
            val outputFormat = when {
                saveAsCSVButton.isSelected -> OutputFormat.CSV
                saveAsXMLButton.isSelected -> OutputFormat.XML
                else -> ""
            }
            runSimpleCellCounter(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                channel,
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                outputFile
                )
        }

        return panel
    }

    /** Runs BatchableColocalizer, called in action listener for "Ok" button. */
    private fun runSimpleColocalizer(
        inputFolder: File?,
        shouldProcessFilesInNestedFolders: Boolean,
        thresholdRadius: Int,
        gaussianBlurSigma: Double,
        outputFormat: String,
        targetChannel: Int,
        transducedChannel: Int,
        allCellsChannel: Int,
        outputFile: File?
    ) {
        if (inputFolder == null) {
            MessageDialog(
                IJ.getInstance(), "Error", "No input folder selected"
            )
        } else if (outputFile == null) {
            MessageDialog(
                IJ.getInstance(), "Error", "No output file selected"
            )
        } else if (!inputFolder.exists()) {
            MessageDialog(
                IJ.getInstance(), "Error",
                "The input folder could not be opened. Please create it if it does not already exist"
            )
        } else {
            val files = getAllFiles(inputFolder, shouldProcessFilesInNestedFolders)
            val colocalizer = BatchableColocalizer(targetChannel, transducedChannel, allCellsChannel, context)
            colocalizer.process(
                openFiles(files),
                CellDiameterRange(0.0, 100.0),
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                outputFile
            )
            MessageDialog(
                IJ.getInstance(),
                "Saved",
                "The batch processing results have successfully been saved to the specified file."
            )
        }
    }

    /** Creates the Simple Colocalizer GUI. */
    private fun simpleColocalizerPanel(): JPanel {
        // TODO: Make this pretty
        val panel = JPanel()
        panel.layout = GridLayout(0, 1)

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

        val colocalizationInstructionLabel = JLabel("<html><div align=\"right\">\nWhen performing batch colocalization, ensure that <br />all input images have the same channel ordering as<br />specified below.</div></html>")
        panel.add(colocalizationInstructionLabel)

        val channel1Model = SpinnerNumberModel(1, 1, 100, 1)
        val targetChannelSpinner = addSpinner(panel, "Cell morphology channel 1", channel1Model)

        val channel2Model = SpinnerNumberModel(0, 0, 100, 1)
        val allCellsChannelSpinner = addSpinner(panel, "Cell morphology channel 2 (0 to disable)", channel2Model)

        val transductionChannel1Model = SpinnerNumberModel(2, 1, 100, 1)
        val transducedChannelSpinner = addSpinner(panel, "Transduction channel", transductionChannel1Model)

        val imageProcessingParametersLabel = JLabel("Image processing parameters")
        panel.add(imageProcessingParametersLabel)

        // TODO: Hook this up so it actually works
        val cellDiameterChannel1Label = JLabel("Cell diameter(px)")
        val cellDiameterChanne1Widget = AlignedTextWidget()
        panel.add(cellDiameterChannel1Label)

        val thresholdRadiusModel = SpinnerNumberModel(20, 1, 1000, 1)
        val thresholdRadiusSpinner = addSpinner(panel, "Local threshold radius", thresholdRadiusModel)

        // TODO: Hook this up so it actually works
        val cellDiameterChannel2Label = JLabel("Cell diameter(px)")
        val cellDiameterChannel2Widget = AlignedTextWidget()
        panel.add(cellDiameterChannel2Label)

        val gaussianBlurModel = SpinnerNumberModel(3, 1, 50, 1)
        val gaussianBlurSpinner = addSpinner(panel, "Gaussian blur sigma", gaussianBlurModel)

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
            val shouldProcessFilesInNestedFolders = shouldProcessFilesInNestedFoldersCheckbox.isSelected
            val thresholdRadius = thresholdRadiusSpinner.value as Int
            val gaussianBlurSigma = (gaussianBlurSpinner.value as Int).toDouble()
            val outputFormat = when {
                saveAsCSVButton.isSelected -> OutputFormat.CSV
                saveAsXMLButton.isSelected -> OutputFormat.XML
                else -> ""
            }
            val targetChannel = targetChannelSpinner.value as Int
            val transducedChannel = transducedChannelSpinner.value as Int
            val allCellsChannel = allCellsChannelSpinner.value as Int
            runSimpleColocalizer(
                inputFolder,
                shouldProcessFilesInNestedFolders,
                thresholdRadius,
                gaussianBlurSigma,
                outputFormat,
                targetChannel,
                transducedChannel,
                allCellsChannel,
                outputFile)
        }

        return panel
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
