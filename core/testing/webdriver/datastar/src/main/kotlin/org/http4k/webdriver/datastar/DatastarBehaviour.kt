package org.http4k.webdriver.datastar

import org.http4k.core.HttpHandler
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
import java.util.Collections.newSetFromMap
import java.util.IdentityHashMap

/**
 * A PageBehaviour simulating a browser running datastar: it maintains a signal store,
 * evaluates data-* expressions, applies datastar-patch-elements/patch-signals SSE events to the
 * live document, and sends non-local signals with every backend action.
 */
class DatastarBehaviour(private val http: HttpHandler) : PageBehaviour {

    private var document = Jsoup.parse("<html><body></body></html>")
    private val store = SignalStore()
    private val initialised: MutableSet<Element> = newSetFromMap(IdentityHashMap())

    /** The live document, reflecting any element patches applied since the page was served. */
    fun pageSource(): String = document.outerHtml()

    override fun pageLoaded(document: Document) {
        this.document = document
        store.clear()
        initialised.clear()
        initialise()
    }

    override fun before(event: PageEvent, element: Element): Boolean = when (event) {
        Click -> fireEvent(element, "click")
        Submit -> element.closestWithAttr("data-on:submit")?.let { fireEvent(it, "submit") } ?: false
        Clear -> clearText(element)
        SendKeys -> false
    }

    override fun after(event: PageEvent, element: Element) {
        when (event) {
            Click -> element.changedControl()?.let(::elementClicked)
            SendKeys -> elementInput(element)
            Clear -> if (element.isCheckable()) elementClicked(element)
            Submit -> {}
        }
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
     * absent or not parseable (falling back to default element behaviour).
     */
    private fun fireEvent(node: Element, event: String): Boolean {
        val handled = run(node.attr("data-on:$event"), node)
        render()
        return handled
    }

    private fun elementInput(node: Element) {
        node.syncBindingInto(store)
        run(node.attr("data-on:input"), node)
        run(node.attr("data-on:change"), node)
        render()
    }

    private fun elementClicked(node: Element) {
        node.syncBindingInto(store)
        run(node.attr("data-on:change"), node)
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
            val nodes = (document.select("[^data-signals], [^data-bind], [^data-indicator]") +
                document.getElementsByAttribute("data-on:load"))
                .distinct()
                .filterNot(initialised::contains)
            if (nodes.isEmpty()) break
            nodes.forEach { node ->
                initialised.add(node)
                applySignalAttributes(node)
                node.indicatorPaths().forEach { if (!store.contains(it)) store[it] = false }
            }
            nodes.forEach { node -> node.initialiseBinding(store) }
            nodes.forEach { node -> run(node.attr("data-on:load"), node) }
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
            response.body.stream.chunkedSseSequence()
                .filterIsInstance<SseMessage.Event>()
                .forEach { event ->
                    when (val patch = runCatching { DatastarEvent.from(event) }.getOrNull()) {
                        is PatchElements -> document.applyPatch(patch)

                        is PatchSignals -> store.patch(
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

/** jsoup CSS selectors mishandle colons in attribute names, so walk ancestors by literal key. */
private fun Element.closestWithAttr(name: String): Element? {
    var current: Element? = this
    while (current != null) {
        if (current.hasAttr(name)) return current
        current = current.parent()
    }
    return null
}

private fun Element.isCheckable() = tagName() == "input" && attr("type") in setOf("checkbox", "radio")

private fun Element.changedControl(): Element? = when {
    tagName() == "option" -> closest("select")
    isCheckable() -> this
    else -> null
}

private fun String.parseModifiers(): Pair<String, Set<String>> =
    split("__").let { it.first() to it.drop(1).toSet() }

internal fun String.kebabPathToCamel() = split('.').joinToString(".") { segment ->
    segment.split('-').filter { it.isNotEmpty() }
        .mapIndexed { index, part -> if (index == 0) part else part.replaceFirstChar(Char::uppercase) }
        .joinToString("")
}
