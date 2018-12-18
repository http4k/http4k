package org.http4k.webdriver

import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement
import org.openqa.selenium.internal.FindsByClassName
import org.openqa.selenium.internal.FindsByCssSelector
import org.openqa.selenium.internal.FindsById
import org.openqa.selenium.internal.FindsByTagName

internal class JSoupElementFinder(private val navigate: Navigate, private val getURL: GetURL, private val element: Element) :
    FindsByCssSelector, FindsByTagName, FindsById, FindsByClassName, SearchContext {
    override fun findElementByClassName(className: String) = findElementsByClassName(className).firstOrNull()

    override fun findElementsByClassName(className: String) = findElementsByCssSelector(".$className")

    override fun findElementByTagName(tagName: String) = findElementsByTagName(tagName).firstOrNull()

    override fun findElementsByTagName(tagName: String) = findElementsByCssSelector(tagName)

    override fun findElementById(id: String) = findElementsById(id).firstOrNull()

    override fun findElementsById(id: String) = findElementsByCssSelector("#$id")

    override fun findElementByCssSelector(selector: String) = findElementsByCssSelector(selector).firstOrNull()

    override fun findElementsByCssSelector(selector: String): List<WebElement> = element.select(selector).map { JSoupWebElement(navigate, getURL, it) }

    override fun findElement(by: By): WebElement? = by.findElement(this)

    override fun findElements(by: By): List<WebElement> = by.findElements(this)
}