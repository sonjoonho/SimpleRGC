package simplergc.services

import org.scijava.table.DefaultGenericTable

/**
 * Outputs the result of the plugin.
 */
interface SimpleOutput {

    companion object {
        const val ARTICLE_CITATION = "[insert full citation]"
        const val PLUGIN_VERSION = "1.0.0"
    }

    fun output()
}

abstract class BaseTable(private val table: DefaultGenericTable) {
    open fun produce(): DefaultGenericTable {
        return table
    }
}
