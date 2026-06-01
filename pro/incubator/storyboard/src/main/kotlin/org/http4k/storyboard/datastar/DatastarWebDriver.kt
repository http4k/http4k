package org.http4k.storyboard.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.datastar.DatastarEvent
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import org.http4k.webdriver.Http4kWebDriver
import org.http4k.webdriver.JSoupElementFinder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.net.URL

private const val FIRED_ATTR = "data-storyboard-on-load-fired"

class DatastarWebDriver(
    private val delegate: Http4kWebDriver,
    private val handler: HttpHandler
) : WebDriver by delegate {

    private var document: Document = Jsoup.parse("<html><body></body></html>")

    override fun get(url: String) {
        delegate.get(url)
        loadFromDelegate()
    }

    override fun navigate(): WebDriver.Navigation = object : WebDriver.Navigation {
        override fun back() { delegate.navigate().back(); loadFromDelegate() }
        override fun forward() { delegate.navigate().forward(); loadFromDelegate() }
        override fun refresh() { delegate.navigate().refresh(); loadFromDelegate() }
        override fun to(url: URL) { delegate.navigate().to(url); loadFromDelegate() }
        override fun to(url: String) { delegate.navigate().to(url); loadFromDelegate() }
    }

    override fun getPageSource(): String = document.outerHtml()

    override fun findElement(by: By): WebElement =
        DatastarWebElement(finder().findElement(by), ::fireAction)

    override fun findElements(by: By): List<WebElement> =
        finder().findElements(by).map { DatastarWebElement(it, ::fireAction) }

    private fun finder() = JSoupElementFinder(::navigateRequest, { delegate.currentUrl }, document)

    private fun navigateRequest(request: Request) {
        val response = handler(request)
        document = Jsoup.parse(response.bodyString())
        fireAllOnLoad()
    }

    private fun loadFromDelegate() {
        document = Jsoup.parse(delegate.pageSource ?: "")
        fireAllOnLoad()
    }

    private fun fireAllOnLoad() {
        var loops = 0
        while (true) {
            val nodes = document.select("[data-on-load]:not([$FIRED_ATTR])")
            if (nodes.isEmpty() || ++loops > 100) return
            nodes.forEach { node ->
                val expression = node.attr("data-on-load")
                node.attr(FIRED_ATTR, "true")
                parseAction(expression)?.let(::fireAction)
            }
        }
    }

    private fun fireAction(action: Action) {
        val response = handler(action.toRequest())
        response.body.stream.chunkedSseSequence()
            .filterIsInstance<SseMessage.Event>()
            .filter { it.event == "datastar-patch-elements" }
            .forEach { event ->
                val patch = DatastarEvent.from(event) as? DatastarEvent.PatchElements ?: return@forEach
                document.applyPatch(patch)
            }
    }
}

internal class DatastarWebElement(
    private val delegate: WebElement,
    private val fireAction: (Action) -> Unit
) : WebElement by delegate {

    override fun click() {
        val expression = delegate.getDomAttribute("data-on-click")
        val action = expression?.let(::parseAction)
        if (action != null) {
            fireAction(action)
            return
        }
        delegate.click()
    }

    override fun findElement(by: By): WebElement =
        DatastarWebElement(delegate.findElement(by), fireAction)

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).map { DatastarWebElement(it, fireAction) }
}
