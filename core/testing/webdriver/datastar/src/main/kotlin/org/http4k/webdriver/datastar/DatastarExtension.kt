package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

/**
 * Extends what the DatastarWebDriver models of a page. It is told about every element a
 * datastar-patch-elements event carries, and can act on the page as a browser running
 * that element would.
 */
fun interface DatastarExtension {
    fun patched(element: Element, page: DatastarPage)
}

