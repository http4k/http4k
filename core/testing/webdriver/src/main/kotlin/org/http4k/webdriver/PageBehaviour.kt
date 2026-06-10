package org.http4k.webdriver

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Extension point allowing a page runtime (e.g. a reactive frontend framework simulation) to hook
 * into Http4kWebDriver page loads and element interactions. All hooks default to no-ops, so a
 * driver without a custom PageBehaviour is unaffected.
 */
interface PageBehaviour {

    /**
     * Called when a page has been loaded (or became current again through history navigation).
     * The document is live: mutations to it are reflected in subsequent element lookups.
     */
    fun pageLoaded(document: Document) {}

    /**
     * Called before the default handling of an element interaction. Return true to mark the
     * event as handled, skipping both the default behaviour and the afterEvent hook.
     */
    fun beforeEvent(element: Element, event: PageEvent): Boolean = false

    /** Called after the default handling of an element interaction. */
    fun afterEvent(element: Element, event: PageEvent) {}

    /**
     * Reports whether an element is displayed, or null if this behaviour has no opinion (in
     * which case WebElement.isDisplayed retains its default unsupported behaviour).
     */
    fun displayed(element: Element): Boolean? = null

    companion object {
        val NoOp = object : PageBehaviour {}
    }
}

enum class PageEvent { Click, Submit, SendKeys, Clear }
