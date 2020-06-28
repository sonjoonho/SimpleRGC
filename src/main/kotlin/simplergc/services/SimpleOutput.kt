package simplergc.services

/**
 * Outputs the result of the plugin.
 */
abstract class SimpleOutput {

    abstract fun output()
}

/**
 * The user can optionally output the results to a file.
 */
object OutputFormat {
    const val DISPLAY = "Display in ImageJ"
    const val CSV = "Save as CSV file"
    const val XML = "Save as XML file"
}