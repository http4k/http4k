package org.http4k.storyboard.datastar

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Re-renders reactive data-* attributes from the signal store. Rendering is pure: any @action
 * inside a reactive expression is ignored rather than dispatched.
 */
internal fun Document.render(store: SignalStore) {
    select("[data-text]").forEach { node ->
        DatastarExpression.parseOrNull(node.attr("data-text"))
            ?.let { node.text(stringify(it.evaluate(store))) }
    }

    select("[data-show]").forEach { node ->
        DatastarExpression.parseOrNull(node.attr("data-show"))
            ?.let { node.show(truthy(it.evaluate(store))) }
    }

    select("[^data-class]").forEach { node ->
        node.attributes().filter { it.key.startsWith("data-class") }.forEach { attribute ->
            DatastarExpression.parseOrNull(attribute.value)?.evaluate(store)?.let { value ->
                when (val name = attribute.key.removePrefix("data-class").removePrefix("-")) {
                    "" -> (value as? Map<*, *>)?.forEach { (cls, on) -> node.toggleClass(cls.toString(), truthy(on)) }
                    else -> node.toggleClass(name, truthy(value))
                }
            }
        }
    }

    select("[^data-attr]").forEach { node ->
        node.attributes().filter { it.key.startsWith("data-attr") }.forEach { attribute ->
            DatastarExpression.parseOrNull(attribute.value)?.let { expr ->
                val value = expr.evaluate(store)
                when (val name = attribute.key.removePrefix("data-attr").removePrefix("-")) {
                    "" -> (value as? Map<*, *>)?.forEach { (attr, v) -> node.setAttr(attr.toString(), v) }
                    else -> node.setAttr(name, value)
                }
            }
        }
    }

    select("[data-bind], [^data-bind-]").forEach { node ->
        node.bindingPath()?.let { path -> node.writeValue(store[path]) }
    }
}

private fun Element.toggleClass(name: String, on: Boolean) {
    if (on) addClass(name) else removeClass(name)
}

private fun Element.setAttr(name: String, value: Any?) {
    when (value) {
        null, false -> removeAttr(name)
        true -> attr(name, "")
        else -> attr(name, stringify(value))
    }
}

/** The signal path an element is two-way bound to, from data-bind="path" or data-bind-path. */
internal fun Element.bindingPath(): String? =
    attr("data-bind").takeIf { it.isNotBlank() }
        ?: attributes().firstOrNull { it.key.startsWith("data-bind-") }
            ?.let { it.key.removePrefix("data-bind-").kebabPathToCamel() }

/** On first sight of a bound element: an existing signal wins, otherwise adopt the element's value. */
internal fun Element.initialiseBinding(store: SignalStore) {
    val path = bindingPath() ?: return
    if (store.contains(path)) writeValue(store[path]) else store[path] = currentValue() ?: ""
}

internal fun Element.syncBindingInto(store: SignalStore) {
    val path = bindingPath() ?: return
    val value = currentValue() ?: return
    store[path] = value
}

private fun Element.currentValue(): Any? = when {
    isCheckbox() -> hasAttr("checked")
    isRadio() -> if (hasAttr("checked")) attr("value") else null
    tagName() == "textarea" -> text()
    tagName() == "select" -> selectFirst("option[selected]")?.optionValue() ?: ""
    else -> attr("value")
}

private fun Element.writeValue(value: Any?) {
    when {
        isCheckbox() -> if (truthy(value)) attr("checked", "checked") else removeAttr("checked")
        isRadio() -> if (stringify(value) == attr("value")) attr("checked", "checked") else removeAttr("checked")
        tagName() == "textarea" -> text(stringify(value))
        tagName() == "select" -> {
            val target = stringify(value)
            select("option").forEach {
                if (it.optionValue() == target) it.attr("selected", "selected") else it.removeAttr("selected")
            }
        }

        else -> attr("value", stringify(value))
    }
}

private fun Element.optionValue() = if (hasAttr("value")) attr("value") else text()
private fun Element.isCheckbox() = tagName() == "input" && attr("type") == "checkbox"
private fun Element.isRadio() = tagName() == "input" && attr("type") == "radio"

private fun Element.show(visible: Boolean) {
    val styles = attr("style").split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.replace(" ", "") != "display:none" }
    val updated = if (visible) styles else styles + "display:none"
    if (updated.isEmpty()) removeAttr("style") else attr("style", updated.joinToString("; "))
}
