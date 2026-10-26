package org.http4k.webdriver

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Extension point allowing a page runtime to hook into Http4kWebDriver page loads and element interactions.
 */
interface PageBehaviour {

    fun pageLoaded(document: Document) {}

    fun beforeFind() {}

    fun pageSource(document: Document): String? = null

    fun before(event: PageEvent, element: Element): Boolean = false

    fun after(event: PageEvent, element: Element) {}

    fun displayed(element: Element): Boolean? = null

    fun closed() {}

    companion object {
        val NoOp = object : PageBehaviour {}
    }
}

enum class PageEvent { Click, Submit, SendKeys, Clear }
