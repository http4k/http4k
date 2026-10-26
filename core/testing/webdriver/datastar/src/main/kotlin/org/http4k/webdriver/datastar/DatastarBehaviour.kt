package org.http4k.webdriver.datastar

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
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
 * A PageBehaviour simulating a browser running datastar: it maintains a signal store,
 * evaluates data-* expressions, applies datastar-patch-elements/patch-signals SSE events to the
 * live document, and sends non-local signals with every backend action.
 */
class DatastarBehaviour(private val http: HttpHandler, private val autoSettle: Boolean = true) : PageBehaviour {

    private var document = Jsoup.parse("<html><body></body></html>")
    private val store = SignalStore()
    private val initialised = IdentityHashMap<Element, String>()
    private val streams = mutableListOf<LiveStream>()
    private val actions = mutableListOf<ActionResult>()
    private val firedOnce = mutableSetOf<Pair<Element, String>>()

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
            if (arrived.isEmpty()) return
            arrived.forEach(::apply)
            initialise()
        }
    }

    override fun pageLoaded(document: Document) {
        closed()
        actions.clear()
        firedOnce.clear()
        this.document = document
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
            Submit -> element.closestWithAttr("data-on:submit")?.let { fireEvent(it, "submit") } ?: false
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
            val event = DomEvent(element, key)
            run(element, "data-on:keydown", event)
            document.allElements.forEach { run(it, "data-on:keydown", event, onWindow = true) }
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

    /**
     * Evaluates the element's data-on-<event> expression. Returns false if the attribute is
     * absent (falling back to default element behaviour).
     */
    private fun fireEvent(node: Element, event: String): Boolean {
        val handled = run(node, "data-on:$event")
        render()
        return handled
    }

    private fun elementInput(node: Element) {
        node.syncBindingInto(store)
        run(node, "data-on:input")
        run(node, "data-on:change")
        render()
    }

    private fun elementClicked(node: Element) {
        node.syncBindingInto(store)
        run(node, "data-on:change")
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

    private fun run(source: Element, name: String, event: DomEvent = DomEvent(source), onWindow: Boolean = false): Boolean {
        val attribute = source.attributes().firstOrNull {
            val (attributeName, modifiers) = it.key.parseModifiers()
            attributeName == name && ("window" in modifiers) == onWindow
        } ?: return false
        if ("once" in attribute.key.parseModifiers().second && !firedOnce.add(source to attribute.key)) return true
        val parsed = DatastarExpression.parseAttribute(attribute.key, attribute.value) ?: return false
        parsed.evaluate(store, event) { action -> fireAction(action, source) }
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
        document.requireModelledAttributes()
        var loops = 0
        while (loops++ < 100) {
            val nodes = (document.select("[^data-signals], [^data-bind], [^data-indicator]") +
                document.getElementsByAttribute("data-on:load") +
                document.getElementsByAttribute("data-init"))
                .distinct()
                .filterNot { initialised[it] == it.datastarAttributes() }
            if (nodes.isEmpty()) break
            nodes.forEach { node ->
                initialised[node] = node.datastarAttributes()
                applySignalAttributes(node)
                node.indicatorPaths().forEach { if (!store.contains(it)) store[it] = false }
            }
            nodes.forEach { node -> node.initialiseBinding(store) }
            nodes.forEach { node ->
                run(node, "data-on:load")
                run(node, "data-init")
            }
        }
        render()
    }

    private fun applySignalAttributes(node: Element) {
        node.attributes()
            .filter { it.key.startsWith("data-signals") }
            .forEach { attribute ->
                val (name, modifiers) = attribute.key.removePrefix("data-signals").parseModifiers()
                val onlyIfMissing = "ifmissing" in modifiers
                val value = DatastarExpression.parseAttribute(attribute.key, attribute.value)?.evaluate(store, dispatch = ::fireAction)
                when {
                    name.isEmpty() -> (value as? Map<*, *>)?.let { store.patch(it.mapKeys { (k, _) -> k.toString() }, onlyIfMissing) }

                    else -> {
                        val path = name.removePrefix(":").kebabPathToCamel()
                        if (!(onlyIfMissing && store.contains(path))) store[path] = value
                    }
                }
            }
    }

    private fun fireAction(action: Action, source: Element? = null) {
        val indicators = source?.indicatorPaths().orEmpty()
        indicators.forEach { store[it] = true }
        try {
            // the dispatch can happen mid-expression, so computed signals may be stale here
            document.recompute(store)
            val response = http(action.toRequest(store.toTransportJson()))
            when (response.body.length) {
                null -> LiveStream(response.body.stream).also { streams += it }.received().forEach(::apply)
                else -> response.body.stream.chunkedSseSequence().forEach(::apply)
            }
        } finally {
            indicators.forEach { store[it] = false }
        }
        initialise()
    }

    private fun apply(message: SseMessage) {
        if (message !is SseMessage.Event) return
        when (val patch = runCatching { DatastarEvent.from(message) }.getOrNull()) {
            is PatchElements -> document.applyPatch(patch)

            is PatchSignals -> store.patch(
                parseJsonObject(patch.signals.joinToString("\n") { it.value }),
                patch.onlyIfMissing ?: false
            )

            null -> {}
        }
    }
}

/** jsoup CSS selectors mishandle colons in attribute names, so walk ancestors by literal key. */
private fun Element.closestWithAttr(name: String): Element? {
    var current: Element? = this
    while (current != null) {
        if (current.attributes().any { it.key.parseModifiers().first == name }) return current
        current = current.parent()
    }
    return null
}

private fun Element.datastarAttributes() = attributes().filter { it.key.startsWith("data-") }.joinToString { it.html() }

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
