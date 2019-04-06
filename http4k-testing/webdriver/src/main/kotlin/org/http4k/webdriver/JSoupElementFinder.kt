package org.http4k.webdriver

import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement

internal class JSoupElementFinder(
    private val navigate: Navigate,
    private val getURL: GetURL,
    private val element: Element
) : SearchContext {
    internal fun findElementsByCssQuery(query: String) = element.select(query).map { JSoupWebElement(navigate, getURL, it) }

    override fun findElement(by: By): WebElement? = by.findElement(this)

    override fun findElements(by: By): List<WebElement> = by.findElements(this)
}
