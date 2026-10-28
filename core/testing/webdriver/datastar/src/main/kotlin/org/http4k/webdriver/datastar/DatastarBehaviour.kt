package org.http4k.webdriver.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.relative
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import org.http4k.webdriver.PageBehaviour
import org.http4k.webdriver.PageEvent
import org.http4k.webdriver.PageEvent.Clear
import org.http4k.webdriver.PageEvent.Click
import org.http4k.webdriver.PageEvent.SendKeys
import org.http4k.webdriver.PageEvent.Submit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.openqa.selenium.Keys
import org.openqa.selenium.Keys.ARROW_DOWN
import org.openqa.selenium.Keys.ARROW_LEFT
import org.openqa.selenium.Keys.ARROW_RIGHT
import org.openqa.selenium.Keys.ARROW_UP
import org.openqa.selenium.Keys.BACK_SPACE
import org.openqa.selenium.Keys.DELETE
import org.openqa.selenium.Keys.DOWN
import org.openqa.selenium.Keys.ENTER
import org.openqa.selenium.Keys.ESCAPE
import org.openqa.selenium.Keys.LEFT
import org.openqa.selenium.Keys.RETURN
import org.openqa.selenium.Keys.RIGHT
import org.openqa.selenium.Keys.SPACE
import org.openqa.selenium.Keys.TAB
import org.openqa.selenium.Keys.UP
import java.util.IdentityHashMap

/**
 * A PageBehaviour simulating a browser running datastar: it keeps a signal store, applies
 * datastar-patch-elements/patch-signals SSE events to the live document, and sends non-local
 * signals with every backend action. It models datastar's attributes through the built-in plugins,
 * plus [plugins], which add to them or replace a built-in of the same name; a navigation a plugin
 * asks for is handed to [navigate], relative to the page's base.
 */
class DatastarBehaviour(
    private val http: HttpHandler,
    private val autoSettle: Boolean = true,
    plugins: List<DatastarPlugin> = emptyList(),
    private val navigate: (Uri) -> Unit = {},
) : PageBehaviour {

    private var document = Jsoup.parse("<html><body></body></html>")
    private val store = SignalStore()
    private val initialised = IdentityHashMap<Element, String>()
    private val streams = mutableListOf<LiveStream>()
    private val actions = mutableListOf<ActionResult>()
    private val firedOnce = mutableSetOf<Pair<Element, String>>()
    private var pendingNavigation: Uri? = null

    private val attributePlugins = (DatastarPlugins.standard + plugins).filterIsInstance<AttributePlugin>()
        .fold(linkedMapOf<String, AttributePlugin>()) { byName, plugin -> byName.apply { put(plugin.name, plugin) } }

    private val watchers = plugins.filterIsInstance<WatcherPlugin>()

    private inner class Page(private val source: Element?, private val rendering: Boolean) : DatastarPage {
        override fun evaluate(attribute: DatastarAttribute, event: DatastarDomEvent?): Any? =
            DatastarExpression.parseAttribute(attribute.qualifiedName, attribute.expression)
                ?.evaluate(store, event) { action -> if (!rendering) fireAction(action, source) }

        override fun signal(path: String) = store[path]

        override fun signal(path: String, value: Any?) {
            store[path] = value
        }

        override fun hasSignal(path: String) = store.contains(path)

        override fun patchSignals(values: Map<String, Any?>, onlyIfMissing: Boolean) = store.patch(values, onlyIfMissing)

        override fun navigate(location: String) {
            val base = document.selectFirst("base[href]")?.attr("href").orEmpty()
            pendingNavigation = Uri.of(base).relative(Uri.of(location))
        }
    }

    private val page: DatastarPage = Page(null, rendering = false)

    internal fun actions(): List<ActionResult> = actions.toList()

    internal fun responded(request: Request, response: Response) {
        if (request.header("datastar-request") != null) actions += ActionResult(request.method, request.uri, response.status)
    }

    fun pageSource(): String {
        autoSettleIfNeeded()
        return document.outerHtml()
    }

    override fun beforeFind() = autoSettleIfNeeded()

    private fun autoSettleIfNeeded() {
        if (autoSettle) settle()
    }

    fun settle() {
        while (true) {
            val arrived = streams.toList().flatMap { it.received() }
            if (arrived.isEmpty()) return followNavigation()
            arrived.forEach(::apply)
            initialise()
        }
    }

    override fun pageLoaded(document: Document) {
        closed()
        actions.clear()
        firedOnce.clear()
        this.document = document
        pendingNavigation = null
        store.clear()
        initialised.clear()
        initialise()
    }

    override fun closed() {
        streams.forEach(LiveStream::close)
        streams.clear()
    }

    override fun before(event: PageEvent, element: Element): Boolean {
        autoSettleIfNeeded()
        return when (event) {
            Click -> fireEvent(element, "click")
            Submit -> generateSequence(element) { it.parent() }.any { offer("submit", it) }.also { render() }
            Clear -> clearText(element)
            SendKeys -> false
        }
    }

    override fun after(event: PageEvent, element: Element) {
        when (event) {
            Click -> element.changedControl()?.let(::elementClicked)
            SendKeys -> elementInput(element)
            Clear -> if (element.isCheckable()) elementClicked(element)
            Submit -> {}
        }
    }

    override fun keysSent(element: Element, keys: List<CharSequence>) {
        keys.joinToString("").map(::domKey).forEach { key ->
            offer("keydown", element, key)
            document.allElements.forEach { offer("keydown", it, key, window = true) }
        }
        render()
    }

    override fun displayed(element: Element): Boolean {
        var current: Element? = element
        while (current != null) {
            if (current.attr("style").replace(" ", "").contains("display:none")) return false
            current = current.parent()
        }
        return true
    }

    /** Offers the event to the element's plugins. Returns false if none handled it (falling back to default element behaviour). */
    private fun fireEvent(node: Element, event: String): Boolean {
        val handled = offer(event, node)
        render()
        return handled
    }

    private fun elementInput(node: Element) {
        offer("input", node)
        offer("change", node)
        render()
    }

    private fun elementClicked(node: Element) {
        offer("change", node)
        render()
    }

    /** JSoupWebElement.clear only resets checkables, so typing-style clears are handled here. */
    private fun clearText(element: Element): Boolean {
        when {
            element.tagName() == "input" && !element.isCheckable() -> element.attr("value", "")
            element.tagName() == "textarea" -> element.text("")
            else -> return false
        }
        elementInput(element)
        return true
    }

    private fun render() = renderPlugins()

    private fun initialisePlugins(nodes: List<Element>) = attributePlugins.values.forEach { plugin ->
        nodes.forEach { node -> node.attributesFor(plugin).forEach { plugin.initialise(node, it, Page(node, rendering = false)) } }
    }

    private fun offer(type: String, element: Element, key: String? = null, window: Boolean = false): Boolean {
        val event = DatastarDomEvent(type, element, key, window)
        val page = Page(element, rendering = false)
        return attributePlugins.values.fold(false) { handled, plugin ->
            element.attributesFor(plugin).fold(handled) { soFar, attribute ->
                val once = "once" in attribute.modifiers
                when {
                    once && (element to attribute.qualifiedName) in firedOnce -> true
                    plugin.handle(event, element, attribute, page) -> true.also { if (once) firedOnce += element to attribute.qualifiedName }
                    else -> soFar
                }
            }
        }
    }

    private fun renderPlugins() {
        val rendering = Page(null, rendering = true)
        repeat(10) {
            val before = store.toJson()
            attributePlugins.values.forEach { plugin ->
                document.allElements.forEach { element ->
                    element.attributesFor(plugin).forEach { plugin.render(element, it, rendering) }
                }
            }
            if (store.toJson() == before) return
        }
    }

    /**
     * Brings the DOM up to date: applies signal initialisation and on-load expressions to any
     * elements not yet seen (including those introduced by element patches).
     */
    private fun initialise() {
        document.requireModelledAttributes(attributePlugins)
        var loops = 0
        while (loops++ < 100) {
            val nodes = (document.select("[^data-signals], [^data-bind], [^data-indicator]") +
                document.getElementsByAttribute("data-on:load") +
                document.getElementsByAttribute("data-init"))
                .distinct()
                .filterNot { initialised[it] == it.datastarAttributes() }
            if (nodes.isEmpty()) break
            nodes.forEach { node -> initialised[node] = node.datastarAttributes() }
            initialisePlugins(nodes)
        }
        render()
    }

    private fun fireAction(action: Action, source: Element? = null) {
        val indicators = source?.indicatorPaths().orEmpty()
        indicators.forEach { store[it] = true }
        try {
            // the dispatch can happen mid-expression, so computed signals may be stale here
            renderPlugins()
            val response = http(action.toRequest(store.toTransportJson()))
            when (response.body.length) {
                null -> LiveStream(response.body.stream).also { streams += it }.received().forEach(::apply)
                else -> response.body.stream.chunkedSseSequence().forEach(::apply)
            }
        } finally {
            indicators.forEach { store[it] = false }
        }
        initialise()
        followNavigation()
    }

    private fun followNavigation() {
        val destination = pendingNavigation ?: return
        pendingNavigation = null
        navigate(destination)
    }

    private fun apply(message: SseMessage) {
        if (message !is SseMessage.Event) return
        when (val patch = runCatching { DatastarEvent.from(message) }.getOrNull()) {
            is PatchElements -> document.applyPatch(patch).forEach { element ->
                watchers.forEach { it.patched(element, page) }
            }

            is PatchSignals -> store.patch(
                parseJsonObject(patch.signals.joinToString("\n") { it.value }),
                patch.onlyIfMissing ?: false
            )

            null -> {}
        }
    }
}

private fun Element.datastarAttributes() = attributes().filter { it.key.startsWith("data-") }.joinToString { it.html() }

internal fun Element.attributesFor(plugin: AttributePlugin): List<DatastarAttribute> =
    attributes().mapNotNull { it.toDatastarAttribute() }.filter { it.name == plugin.name }

internal fun org.jsoup.nodes.Attribute.toDatastarAttribute(): DatastarAttribute? {
    if (!key.startsWith("data-")) return null
    val (base, modifiers) = key.parseModifiers()
    val plugin = base.removePrefix("data-")
    return DatastarAttribute(key, plugin.substringBefore(':'), plugin.substringAfter(':', "").ifEmpty { null }, modifiers, value)
}

private fun domKey(char: Char): String = when (val key = Keys.getKeyFromUnicode(char)) {
    null -> char.toString()
    ENTER, RETURN -> "Enter"
    ESCAPE -> "Escape"
    TAB -> "Tab"
    BACK_SPACE -> "Backspace"
    DELETE -> "Delete"
    SPACE -> " "
    UP, ARROW_UP -> "ArrowUp"
    DOWN, ARROW_DOWN -> "ArrowDown"
    LEFT, ARROW_LEFT -> "ArrowLeft"
    RIGHT, ARROW_RIGHT -> "ArrowRight"
    else -> throw UnsupportedDatastarExpression("data-on:keydown", "Keys.$key")
}

private fun Element.isCheckable() = tagName() == "input" && attr("type") in setOf("checkbox", "radio")

private fun Element.changedControl(): Element? = when {
    tagName() == "option" -> closest("select")
    isCheckable() -> this
    else -> null
}

internal fun String.parseModifiers(): Pair<String, Set<String>> =
    split("__").let { parts -> parts.first() to parts.drop(1).map { it.substringBefore('.') }.toSet() }

internal fun String.kebabPathToCamel() = split('.').joinToString(".") { segment ->
    segment.split('-').filter { it.isNotEmpty() }
        .mapIndexed { index, part -> if (index == 0) part else part.replaceFirstChar(Char::uppercase) }
        .joinToString("")
}
