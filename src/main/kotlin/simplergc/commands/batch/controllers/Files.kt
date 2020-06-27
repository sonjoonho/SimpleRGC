package simplergc.commands.batch.controllers

import ij.IJ
import ij.ImagePlus
import ij.gui.MessageDialog
import ij.io.Opener
import java.io.File
import loci.formats.UnknownFormatException
import loci.plugins.BF
import loci.plugins.`in`.ImporterOptions

fun getAllFiles(file: File, shouldProcessFilesInNestedFolders: Boolean): List<File> {
    return if (shouldProcessFilesInNestedFolders) {
        file.walkTopDown().filter { f -> !f.isDirectory }.toList()
    } else {
        file.listFiles()?.filter { f -> !f.isDirectory }?.toList() ?: listOf(file)
    }
}

fun openFiles(inputFiles: List<File>): List<ImagePlus> {
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
            // TODO(sonjoonho): Using LogService would be preferable here.
            println("Skipping file with unsupported type \"${file.name}\"")
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
