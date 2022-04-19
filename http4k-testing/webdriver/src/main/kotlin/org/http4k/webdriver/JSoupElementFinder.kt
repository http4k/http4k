package org.http4k.webdriver

import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement

class JSoupElementFinder(
    private val navigate: Navigate,
    private val getURL: GetURL,
    private val element: Element
) : SearchContext {
    internal fun findElementsByCssQuery(query: String) =
        element.select(query).map { JSoupWebElement(navigate, getURL, it) }

    override fun findElement(by: By): WebElement? = when (by) {
        is By.ById -> cssSelector("#${by.remoteParameters.value()}").findElement(this)
        is By.ByClassName -> cssSelector(".${by.remoteParameters.value()}").findElement(this)
        is By.ByTagName -> cssSelector(by.remoteParameters.value().toString()).findElement(this)
        is By.ByCssSelector -> cssSelector(by.remoteParameters.value().toString()).findElement(this)
        else -> error("unsupported By ${by::class.java}")
    }

    private fun cssSelector(cssSelector: String) = object : By() {
        override fun findElements(context: SearchContext) = when (context) {
            is JSoupElementFinder -> context.findElementsByCssQuery(cssSelector)
            else -> throw UnsupportedOperationException(
                "This By implementation only supports the http4k JSoupElementFinder SearchContext"
            )
        }
    }

    override fun findElements(by: By): List<WebElement> = when (by) {
        is By.ById -> cssSelector("#${by.remoteParameters.value()}").findElements(this)
        is By.ByClassName -> cssSelector(".${by.remoteParameters.value()}").findElements(this)
        is By.ByTagName -> cssSelector(by.remoteParameters.value().toString()).findElements(this)
        is By.ByCssSelector -> cssSelector(by.remoteParameters.value().toString()).findElements(this)
        else -> error("unsupported By ${by::class.java}")
    }
}
