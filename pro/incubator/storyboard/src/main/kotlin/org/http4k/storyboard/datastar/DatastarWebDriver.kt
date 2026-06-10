package org.http4k.storyboard.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.datastar.DatastarEvent
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import org.http4k.webdriver.Http4kWebDriver
import org.http4k.webdriver.JSoupElementFinder
import org.http4k.webdriver.JSoupWebElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.net.URL
import java.util.Collections.newSetFromMap
import java.util.IdentityHashMap

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
    private val initialised: MutableSet<Element> = newSetFromMap(IdentityHashMap())

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
        loadPage(handler(request).bodyString())
    }

    private fun loadFromDelegate() {
        loadPage(delegate.pageSource ?: "")
    }

    private fun loadPage(html: String) {
        document = Jsoup.parse(html)
        store.clear()
        initialised.clear()
        initialise()
    }

    /**
     * Evaluates the element's data-on-<event> expression. Returns false if the attribute is
     * absent or not parseable (allowing callers to fall back to default element behaviour).
     */
    internal fun fireEvent(node: Element, event: String): Boolean {
        val handled = run(node.attr("data-on-$event"), node)
        render()
        return handled
    }

    /** Fired when the value of an element changes through typing (sendKeys/clear). */
    internal fun elementInput(node: Element) {
        node.syncBindingInto(store)
        run(node.attr("data-on-input"), node)
        run(node.attr("data-on-change"), node)
        render()
    }

    /** Fired when the state of a checkbox/radio/select changes through clicking. */
    internal fun elementChanged(node: Element) {
        node.syncBindingInto(store)
        run(node.attr("data-on-change"), node)
        render()
    }

    private fun run(expression: String, source: Element?): Boolean {
        val parsed = expression.takeIf { it.isNotBlank() }?.let(DatastarExpression::parseOrNull) ?: return false
        parsed.evaluate(store) { action -> fireAction(action, source) }
        return true
    }

    private fun render() {
        document.recompute(store)
        document.render(store)
    }

    /**
     * Brings the DOM up to date: applies signal initialisation and on-load expressions to any
     * elements not yet seen (including those introduced by element patches).
     */
    private fun initialise() {
        var loops = 0
        while (loops++ < 100) {
            val nodes = document
                .select("[^data-signals], [^data-bind], [^data-indicator], [data-on-load]")
                .filterNot(initialised::contains)
            if (nodes.isEmpty()) break
            nodes.forEach { node ->
                initialised.add(node)
                applySignalAttributes(node)
                node.indicatorPaths().forEach { if (!store.contains(it)) store[it] = false }
            }
            nodes.forEach { node -> node.initialiseBinding(store) }
            nodes.forEach { node -> run(node.attr("data-on-load"), node) }
        }
        render()
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

    internal fun fireAction(action: Action, source: Element? = null) {
        val indicators = source?.indicatorPaths().orEmpty()
        indicators.forEach { store[it] = true }
        try {
            // the dispatch can happen mid-expression, so computed signals may be stale here
            document.recompute(store)
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
        } finally {
            indicators.forEach { store[it] = false }
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
        val node = jsoup()
        if (node != null && driver.fireEvent(node, "click")) return
        val control = node?.let {
            when {
                it.tagName() == "option" -> it.closest("select")
                it.tagName() == "input" && it.attr("type") in setOf("checkbox", "radio") -> it
                else -> null
            }
        }
        delegate.click()
        control?.let(driver::elementChanged)
    }

    override fun sendKeys(vararg keysToSend: CharSequence) {
        delegate.sendKeys(*keysToSend)
        jsoup()?.let(driver::elementInput)
    }

    override fun clear() {
        val node = jsoup()
        when {
            node == null -> delegate.clear()
            node.tagName() == "input" && node.attr("type") in setOf("checkbox", "radio") -> {
                delegate.clear()
                driver.elementChanged(node)
            }

            node.tagName() == "input" -> {
                node.attr("value", "")
                driver.elementInput(node)
            }

            node.tagName() == "textarea" -> {
                node.text("")
                driver.elementInput(node)
            }

            else -> delegate.clear()
        }
    }

    override fun submit() {
        val handler = jsoup()?.closest("[data-on-submit]")
        if (handler != null && driver.fireEvent(handler, "submit")) return
        delegate.submit()
    }

    override fun isDisplayed(): Boolean {
        var current = jsoup()
        while (current != null) {
            if (current.attr("style").replace(" ", "").contains("display:none")) return false
            current = current.parent()
        }
        return true
    }

    override fun getDomAttribute(name: String): String? = delegate.getDomAttribute(name)

    override fun findElement(by: By): WebElement =
        DatastarWebElement(delegate.findElement(by), driver)

    override fun findElements(by: By): List<WebElement> =
        delegate.findElements(by).map { DatastarWebElement(it, driver) }

    private fun jsoup(): Element? = (delegate as? JSoupWebElement)?.element
}
