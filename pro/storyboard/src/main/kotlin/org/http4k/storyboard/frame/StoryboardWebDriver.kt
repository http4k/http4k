package org.http4k.storyboard.frame

import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.util.gzipBase64Encode
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.pagefactory.ByChained
import java.time.Clock

/**
 * Create a [StoryboardWebDriver] that will record all interactions with the page.
 */
fun Storyboard.webDriver(
    http: HttpHandler,
    clock: Clock = Clock.systemUTC()
) = StoryboardWebDriver(
    Http4kWebDriver(ClientFilters.OpenTelemetryTracing(otel).then(http), clock),
    this,
    LocalAssetCollector(http)
)

class StoryboardWebDriver internal constructor(
    private val delegate: WebDriver,
    val storyboard: Storyboard,
    private val assetCollector: LocalAssetCollector? = null
) : WebDriver by delegate {

    fun snapshot(title: String, notes: String = "", level: Level = Story) {
        storyboard.capture(buildFrame(title, notes, level))
    }

    override fun findElement(by: By): WebElement =
        StoryboardWebElement(listOf(by), delegate.findElement(by), ::recordInteraction, by.toString())

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(listOf(by), e, ::recordInteraction, "$by[$i]")
        }

    private fun recordInteraction(title: String) {
        storyboard.capture(buildFrame(title, "", Detail))
    }

    private fun buildFrame(title: String, notes: String, level: Level): StoryFrame {
        val source = pageSource ?: ""
        val assets = assetCollector?.collect(source, delegate.currentUrl) ?: emptyMap()
        return StoryFrame(title, notes, source.gzipBase64Encode(), level, domAssets = assets)
    }
}

class StoryboardWebElement(
    private val selectors: List<By>,
    private val delegate: WebElement,
    private val capture: (String) -> Unit,
    private val description: String
) : WebElement by delegate {

    val selector: By = ByChained(*selectors.toTypedArray())

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
        StoryboardWebElement(selectors + by, delegate.findElement(by), capture, "$description -> $by")

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).mapIndexed { i, e ->
            StoryboardWebElement(selectors + by, e, capture, "$description -> $by[$i]")
        }
}
