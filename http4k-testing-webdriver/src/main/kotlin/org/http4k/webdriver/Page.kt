package org.http4k.webdriver

import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.util.*

data class Page(val handle: UUID, val url: String, val contents: String, val previous: Page? = null, val next: Page? = null) {

    private val parsed = Jsoup.parse(contents)

    fun findElement(by: By) = findElements(by).firstOrNull()

    fun findElements(by: By): List<WebElement> = by.findElements(JSoupElementFinder(parsed))

    fun firstElement(): WebElement? = parsed.body().children().firstOrNull()?.let(::JSoupWebElement)

    val title: String = parsed.title()
}