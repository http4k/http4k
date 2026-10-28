package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

/** The signal paths flagged true while a request fired from this element is in flight. */
internal fun Element.indicatorPaths(): List<String> =
    attributes().filter { it.key.startsWith("data-indicator") }.mapNotNull { attribute ->
        when (val name = attribute.key.removePrefix("data-indicator").removePrefix(":")) {
            "" -> attribute.value.takeIf { it.isNotBlank() }
            else -> name.kebabPathToCamel()
        }
    }

internal fun Element.currentValue(): Any? = when {
    isCheckbox() -> hasAttr("checked")
    isRadio() -> if (hasAttr("checked")) attr("value") else null
    else -> domValue()
}

internal fun Element.domValue(): String = when {
    tagName() == "textarea" -> text()
    tagName() == "select" -> selectFirst("option[selected]")?.optionValue() ?: ""
    else -> attr("value")
}

internal fun Element.writeValue(value: Any?) {
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
