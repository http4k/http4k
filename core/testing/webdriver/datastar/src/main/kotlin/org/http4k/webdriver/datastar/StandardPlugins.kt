package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

internal object Text : AttributePlugin {
    override val name = "text"

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        element.text(stringify(page.evaluate(attribute)))
    }
}

internal object Show : AttributePlugin {
    override val name = "show"

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val styles = element.attr("style").split(';').map { it.trim() }.filter { it.isNotEmpty() && it.replace(" ", "") != "display:none" }
        val updated = if (truthy(page.evaluate(attribute))) styles else styles + "display:none"
        if (updated.isEmpty()) element.removeAttr("style") else element.attr("style", updated.joinToString("; "))
    }
}

internal object Classes : AttributePlugin {
    override val name = "class"

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) =
        keyed(attribute, page.evaluate(attribute)) { name, value ->
            if (truthy(value)) element.addClass(name) else element.removeClass(name)
        }
}

internal object Attrs : AttributePlugin {
    override val name = "attr"

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) =
        keyed(attribute, page.evaluate(attribute)) { name, value ->
            when (value) {
                null, false -> element.removeAttr(name)
                true -> element.attr(name, "")
                else -> element.attr(name, stringify(value))
            }
        }
}

internal object Computed : AttributePlugin {
    override val name = "computed"

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val path = attribute.key?.kebabPathToCamel() ?: return
        val value = page.evaluate(attribute)
        if (page.signal(path) != value) page.signal(path, value)
    }
}

/** A keyed attribute names its target in the key (data-class:active) or as the keys of an object (data-class="{active: …}"). */
private fun keyed(attribute: DatastarAttribute, value: Any?, apply: (String, Any?) -> Unit) {
    when (val key = attribute.key) {
        null -> if (value is Map<*, *>) value.forEach { (name, v) -> apply(name.toString(), v) }
        else -> apply(key, value)
    }
}

internal object Bind : AttributePlugin {
    override val name = "bind"

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val path = bindingPath(attribute) ?: return
        if (page.hasSignal(path)) element.writeValue(page.signal(path)) else page.signal(path, element.currentValue() ?: "")
    }

    override fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val path = bindingPath(attribute) ?: return
        element.writeValue(page.signal(path))
    }

    override fun handle(event: DatastarDomEvent, element: Element, attribute: DatastarAttribute, page: DatastarPage): Boolean {
        if (event.type !in setOf("input", "change")) return false
        val path = bindingPath(attribute) ?: return false
        element.currentValue()?.let { page.signal(path, it) }
        return false
    }

    private fun bindingPath(attribute: DatastarAttribute) = attribute.key?.kebabPathToCamel() ?: attribute.expression.takeIf { it.isNotBlank() }
}

internal object Signals : AttributePlugin {
    override val name = "signals"

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val onlyIfMissing = "ifmissing" in attribute.modifiers
        val value = page.evaluate(attribute)
        when (val key = attribute.key) {
            null -> if (value is Map<*, *>) page.patchSignals(value.mapKeys { (name, _) -> name.toString() }, onlyIfMissing)

            else -> {
                val path = key.kebabPathToCamel()
                if (!(onlyIfMissing && page.hasSignal(path))) page.signal(path, value)
            }
        }
    }
}

internal object Indicator : AttributePlugin {
    override val name = "indicator"

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        val path = attribute.key?.kebabPathToCamel() ?: attribute.expression.takeIf { it.isNotBlank() } ?: return
        if (!page.hasSignal(path)) page.signal(path, false)
    }
}

internal object Init : AttributePlugin {
    override val name = "init"

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        page.evaluate(attribute, DatastarDomEvent("init", element))
    }
}

internal object On : AttributePlugin {
    override val name = "on"

    override fun models(attribute: DatastarAttribute) = isModelledHandler(attribute.key.orEmpty(), attribute.modifiers)

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        if (attribute.key == "load") page.evaluate(attribute, DatastarDomEvent("load", element))
    }

    override fun handle(event: DatastarDomEvent, element: Element, attribute: DatastarAttribute, page: DatastarPage): Boolean {
        if (attribute.key != event.type || ("window" in attribute.modifiers) != event.window) return false
        page.evaluate(attribute, event)
        return true
    }
}

internal object IgnoreMorph : AttributePlugin {
    override val name = "ignore-morph"
}
