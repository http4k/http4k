package org.http4k.webdriver

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.InputStream

internal fun isNotImplemented(fn: () -> Unit) = assertThat({ fn() }, throws<FeatureNotImplementedYet>())

internal fun InputStream.asString(): String {
    return reader().use { it.readText() }
}

internal fun WebDriver.assertOnPage(expected: String) {
    assertThat(findElement(By.tagName("h1"))!!.text, equalTo(expected))
}

internal fun Http4kWebDriver.assertCurrentUrl(expectedUrl: String) {
    assertThat(this, hasCurrentUrl(expectedUrl))
}

internal fun hasCurrentUrl(url: String): Matcher<WebDriver> = object : Matcher<WebDriver> {
    override val description: String = "has the current url of \"$url\""

    override fun invoke(actual: WebDriver): MatchResult {
        return equalTo(url)(actual.currentUrl)
    }
}

internal fun hasElement(by: By, matcher: Matcher<WebElement>): Matcher<SearchContext> = object :
    Matcher<SearchContext> {
    override val description: String = "has the element matching ${by} that " + matcher.description

    override fun invoke(actual: SearchContext): MatchResult {
        val element: WebElement? = actual.findElement(by)

        return when (element) {
            null -> MatchResult.Mismatch("could not find element")
            else -> matcher(element)
        }
    }
}

internal fun hasText(matcher: Matcher<String>): Matcher<WebElement> = object : Matcher<WebElement> {
    override val description: String = "has the text content " + matcher.description

    override fun invoke(actual: WebElement): MatchResult {
        val text : String? = actual.text

        return when (text) {
            null -> MatchResult.Mismatch("could not find any text content")
            else -> matcher(text)
        }
    }
}
