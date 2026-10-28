package org.http4k.webdriver.datastar

/**
 * The page as a plugin can act on it.
 */
interface DatastarPage {
    /** Evaluates the attribute's expression; actions in it are sent unless rendering. */
    fun evaluate(attribute: DatastarAttribute, event: DatastarDomEvent? = null): Any?

    fun signal(path: String): Any?

    fun signal(path: String, value: Any?)

    fun hasSignal(path: String): Boolean

    /** Deep-merges the values into the signals. A null value deletes the signal, unless [onlyIfMissing]. */
    fun patchSignals(values: Map<String, Any?>, onlyIfMissing: Boolean = false)

    /**
     * Loads [location], resolved against the page's base as a link would be, once the action or
     * stream update in progress has been applied.
     */
    fun navigate(location: String)
}
