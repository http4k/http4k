package org.http4k.webdriver.datastar

import org.jsoup.nodes.Document

private val datastarAttributes = setOf(
    "attr", "bind", "class", "computed", "effect", "ignore", "ignore-morph", "indicator", "init", "json-signals",
    "on", "on-intersect", "on-interval", "on-signal-patch", "on-signal-patch-filter", "preserve-attr", "ref",
    "show", "signals", "style", "text",
    "animate", "custom-validity", "match-media", "on-raf", "on-resize", "persist", "query-string", "replace-url",
    "scroll-into-view", "view-transition"
)

private val modelledAttributes = setOf("attr", "bind", "class", "computed", "ignore-morph", "indicator", "init", "on", "show", "signals", "text")

private val modelledEvents = setOf("click", "submit", "input", "change", "load", "keydown")

private val modelledModifiers = setOf("once", "debounce", "throttle", "delay", "viewtransition", "passive", "capture", "prevent", "stop", "case")

internal fun Document.requireModelledAttributes() = allElements.flatMap { it.attributes() }.forEach { attribute ->
    val (name, modifiers) = attribute.key.parseModifiers()
    val plugin = name.removePrefix("data-").substringBefore(':')
    val unmodelled = when {
        !name.startsWith("data-") || plugin !in datastarAttributes -> false
        plugin !in modelledAttributes -> true
        plugin == "on" -> !isModelledHandler(name.substringAfter(':'), modifiers)
        else -> false
    }
    if (unmodelled) throw UnsupportedDatastarExpression(attribute.key, attribute.value)
}

private fun isModelledHandler(event: String, modifiers: Set<String>) =
    event in modelledEvents && (if (event == "keydown") modelledModifiers + "window" else modelledModifiers).containsAll(modifiers)
