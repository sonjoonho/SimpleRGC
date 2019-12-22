package simplecolocalization.commands.batch

import java.io.File

interface Batchable {
    fun process(inputFiles: List<File>, outputFile: File)
}
