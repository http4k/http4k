package org.http4k.webdriver

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Extension point allowing a page runtime to hook into Http4kWebDriver page loads and element interactions.
 */
interface PageBehaviour {

    /**
     * Called when a page has been loaded (or became current again through history navigation).
     * The document is live: mutations to it are reflected in subsequent element lookups.
     */
    fun pageLoaded(document: Document) {}

    fun before(event: PageEvent, element: Element): Boolean = false

    fun after(event: PageEvent, element: Element) {}

    /**
     * Reports whether an element is displayed, or null if this behaviour has no opinion.
     */
    fun displayed(element: Element): Boolean? = null

    companion object {
        val NoOp = object : PageBehaviour {}
    }
}

enum class PageEvent { Click, Submit, SendKeys, Clear }
