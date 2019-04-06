package org.http4k.webdriver

import org.openqa.selenium.By
import org.openqa.selenium.SearchContext

/**
 * Custom set of By implementations for testing http4k applications. As the backing store is JSoup we are limited
 * to implementing selectors which are supported by that.
 */
object By {

    fun tagName(tagName: String) = cssSelector(tagName)

    fun className(className: String) = cssSelector(".$className")

    fun id(id: String) = cssSelector("#$id")

    fun disabledCssSelector(disabledCssSelector: String) = cssSelector("$disabledCssSelector[disabled]")

    fun cssSelector(cssSelector: String) = object : By() {
        override fun findElements(context: SearchContext) = when (context) {
            is JSoupElementFinder -> context.findElementsByCssQuery(cssSelector)
            else -> throw UnsupportedOperationException(
                "This By implementation only supports the http4k JSoupElementFinder SearchContext")
        }
    }
}