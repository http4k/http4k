package org.http4k.webdriver

import org.http4k.core.Status
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.util.UUID

data class Page(val status: Status, private val navigate: Navigate, private val getURL: GetURL, val handle: UUID, val url: String, val contents: String, val previous: Page? = null, val next: Page? = null) {

    private val parsed = Jsoup.parse(contents)

    fun findElement(by: By) = findElements(by).firstOrNull()

    fun findElements(by: By): List<WebElement> = by.findElements(JSoupElementFinder(navigate, getURL, parsed))

    fun firstElement() = parsed.body().children().firstOrNull()?.let { JSoupWebElement(navigate, getURL, it) }

    val title: String = parsed.title()
}