package org.http4k.storyboard.frame

import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.Storyboard
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Clock

/**
 * Create a [StoryboardWebDriver] that will record all interactions with the page.
 */
fun Storyboard.webDriver(
    http: HttpHandler,
    clock: Clock = Clock.systemUTC()
) = StoryboardWebDriver(Http4kWebDriver(ClientFilters.OpenTelemetryTracing(otel).then(http), clock), this)

class StoryboardWebDriver internal constructor(
    private val delegate: WebDriver,
    val storyboard: Storyboard
) : WebDriver by delegate {

    fun capture(title: String, notes: String = "", level: Level = Story) {
        storyboard.capture(WebDriverCapture(title, notes, pageSource?.base64Encode() ?: "", level))
    }

    override fun findElement(by: By): WebElement =
        StoryboardWebElement(delegate.findElement(by), ::recordInteraction, by.toString())

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(e, ::recordInteraction, "$by[$i]")
        }

    private fun recordInteraction(title: String) {
        storyboard.capture(WebDriverCapture(title, "", pageSource?.base64Encode() ?: "", Detail))
    }
}

class StoryboardWebElement(
    private val delegate: WebElement,
    private val capture: (String) -> Unit,
    private val description: String
) : WebElement by delegate {

    override fun click() {
        delegate.click()
        capture("click [$description]")
    }

    override fun submit() {
        delegate.submit()
        capture("submit [$description]")
    }

    override fun clear() {
        delegate.clear()
        capture("clear [$description]")
    }

    override fun sendKeys(vararg keysToSend: CharSequence) {
        delegate.sendKeys(*keysToSend)
        capture("sendKeys '${keysToSend.joinToString("")}' [$description]")
    }

    override fun findElement(by: By): WebElement =
        StoryboardWebElement(delegate.findElement(by), capture, "$description -> $by")

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(e, capture, "$description -> $by[$i]")
        }
}

/** DOM snapshot of a page driven by a `StoryboardWebDriver` — the bread-and-butter capture variant. */
data class WebDriverCapture(
    override val title: String,
    override val notes: String,
    override val dom: String,
    override val level: StoryFrame.Level
) : StoryFrame
