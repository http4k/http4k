package org.http4k.webdriver.datastar

import org.jsoup.nodes.Document

private val datastarAttributes = setOf(
    "attr", "bind", "class", "computed", "effect", "ignore", "ignore-morph", "indicator", "init", "json-signals",
    "on", "on-intersect", "on-interval", "on-signal-patch", "on-signal-patch-filter", "preserve-attr", "ref",
    "show", "signals", "style", "text",
    "animate", "custom-validity", "match-media", "on-raf", "on-resize", "persist", "query-string", "replace-url",
    "scroll-into-view", "view-transition"
)

private val modelledEvents = setOf("click", "submit", "input", "change", "load", "keydown")

private val modelledModifiers = setOf("once", "debounce", "throttle", "delay", "viewtransition", "passive", "capture", "prevent", "stop", "case")

internal fun Document.requireModelledAttributes(plugins: Map<String, AttributePlugin>) =
    allElements.flatMap { it.attributes() }.forEach { raw ->
        val attribute = raw.toDatastarAttribute() ?: return@forEach
        val unmodelled = when (val plugin = plugins[attribute.name]) {
            null -> attribute.name in datastarAttributes
            else -> !plugin.models(attribute)
        }
        if (unmodelled) throw UnsupportedDatastarExpression(raw.key, raw.value)
    }

internal fun isModelledHandler(event: String, modifiers: Set<String>) =
    event in modelledEvents && (if (event == "keydown") modelledModifiers + "window" else modelledModifiers).containsAll(modifiers)
