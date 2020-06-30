package simplergc.services

/**
 * Outputs the result of the plugin.
 */
abstract class SimpleOutput {

    companion object {
        const val ARTICLE_CITATION = "[insert full citation]"
    }

    abstract fun output()
}
