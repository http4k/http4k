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
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.net.URL

private const val INIT_ATTR = "data-storyboard-initialised"

/**
 * A headless, pure-Kotlin approximation of a browser running datastar v1: it maintains a signal
 * store, evaluates data-* expressions, applies datastar-patch-elements/patch-signals SSE events,
 * and sends non-local signals with every backend action.
 */
class DatastarWebDriver(
    private val delegate: Http4kWebDriver,
    private val handler: HttpHandler
) : WebDriver by delegate {

    private var document: Document = Jsoup.parse("<html><body></body></html>")
    private val store = SignalStore()

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
        DatastarWebElement(finder().findElement(by), this)

    override fun findElements(by: By): List<WebElement> =
        finder().findElements(by).map { DatastarWebElement(it, this) }

    private fun finder() = JSoupElementFinder(::navigateRequest, { delegate.currentUrl }, document)

    private fun navigateRequest(request: Request) {
        val response = handler(request)
        document = Jsoup.parse(response.bodyString())
        store.clear()
        initialise()
    }

    private fun loadFromDelegate() {
        document = Jsoup.parse(delegate.pageSource ?: "")
        store.clear()
        initialise()
    }

    /**
     * Evaluates the data-* expression. Returns false if it is not parseable as a datastar
     * expression (allowing callers to fall back to default element behaviour).
     */
    internal fun execute(expression: String): Boolean {
        val parsed = DatastarExpression.parseOrNull(expression) ?: return false
        parsed.evaluate(store, ::fireAction)
        return true
    }

    /**
     * Brings the DOM up to date: applies signal initialisation and on-load expressions to any
     * elements not yet seen (including those introduced by element patches).
     */
    private fun initialise() {
        var loops = 0
        while (loops++ < 100) {
            val nodes = document.select("[^data-signals]:not([$INIT_ATTR]), [data-on-load]:not([$INIT_ATTR])")
            if (nodes.isEmpty()) break
            nodes.forEach { node ->
                node.attr(INIT_ATTR, "true")
                applySignalAttributes(node)
            }
            nodes.forEach { node ->
                node.attr("data-on-load").takeIf { it.isNotBlank() }?.let(::execute)
            }
        }
    }

    private fun applySignalAttributes(node: Element) {
        node.attributes()
            .filter { it.key.startsWith("data-signals") }
            .forEach { attribute ->
                val (name, modifiers) = attribute.key.removePrefix("data-signals").parseModifiers()
                val onlyIfMissing = "ifmissing" in modifiers
                val value = DatastarExpression.parseOrNull(attribute.value)?.evaluate(store, ::fireAction)
                when {
                    name.isEmpty() -> (value as? Map<*, *>)?.let { store.patch(it.mapKeys { (k, _) -> k.toString() }, onlyIfMissing) }
                    else -> {
                        val path = name.removePrefix("-").kebabPathToCamel()
                        if (!(onlyIfMissing && store.contains(path))) store[path] = value
                    }
                }
            }
    }

    internal fun fireAction(action: Action) {
        val response = handler(action.toRequest(store.toTransportJson()))
        response.body.stream.chunkedSseSequence()
            .filterIsInstance<SseMessage.Event>()
            .forEach { event ->
                when (val patch = runCatching { DatastarEvent.from(event) }.getOrNull()) {
                    is DatastarEvent.PatchElements -> document.applyPatch(patch)
                    is DatastarEvent.PatchSignals -> store.patch(
                        parseJsonObject(patch.signals.joinToString("\n") { it.value }),
                        patch.onlyIfMissing ?: false
                    )

                    null -> {}
                }
            }
        initialise()
    }
}

private fun String.parseModifiers(): Pair<String, Set<String>> =
    split("__").let { it.first() to it.drop(1).toSet() }

internal fun String.kebabPathToCamel() = split('.').joinToString(".") { segment ->
    segment.split('-').filter { it.isNotEmpty() }
        .mapIndexed { index, part -> if (index == 0) part else part.replaceFirstChar(Char::uppercase) }
        .joinToString("")
}

internal class DatastarWebElement(
    private val delegate: WebElement,
    private val driver: DatastarWebDriver
) : WebElement by delegate {

    override fun click() {
        val expression = delegate.getDomAttribute("data-on-click")
        if (!expression.isNullOrBlank() && driver.execute(expression)) return
        delegate.click()
    }

    override fun findElement(by: By): WebElement =
        DatastarWebElement(delegate.findElement(by), driver)

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).map { DatastarWebElement(it, driver) }
}
