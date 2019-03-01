package org.http4k.webdriver

import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement

class JSoupElementFinder(private val navigate: Navigate, private val getURL: GetURL, private val element: Element) :
    FindsByCssSelector, FindsByTagName, FindsById, FindsByClassName, SearchContext {
    override fun findElementByClassName(className: String) = findElementsByClassName(className).firstOrNull()

    override fun findElementsByClassName(className: String) = findElementsByCssSelector(".$className")

    override fun findElementByTagName(tagName: String) = findElementsByTagName(tagName).firstOrNull()

    override fun findElementsByTagName(tagName: String) = findElementsByCssSelector(tagName)

    override fun findElementById(id: String) = findElementsById(id).firstOrNull()
internal class JSoupElementFinder(private val navigate: Navigate, private val getURL: GetURL, private val element: Element) :
        SearchContext {

    internal fun findElementsByCssQuery(query: String) = element.select(query).map { JSoupWebElement(navigate, getURL, it) }

    override fun findElement(by: By): WebElement? = by.findElement(this)

    override fun findElements(by: By): List<WebElement> = by.findElements(this)
}
