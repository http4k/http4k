package org.http4k.webdriver.datastar

/**
 * The page a patched element arrived in, as an extension can act on it.
 */
interface DatastarPage {
    /**
     * Loads [location], resolved against the page's base as a link would be, once the action
     * or stream update that carried the patch has been applied.
     */
    fun navigate(location: String)
}
