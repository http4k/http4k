/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.webdriver.datastar.patched

import org.http4k.webdriver.datastar.AttributePlugin
import org.http4k.webdriver.datastar.DatastarAttribute
import org.http4k.webdriver.datastar.DatastarDomEvent
import org.http4k.webdriver.datastar.DatastarPage
import org.jsoup.nodes.Element

object DragEvents : AttributePlugin {
    override val name = "on"

    private val onWindow = setOf("keydown", "dragenter", "dragover", "dragleave", "drop")
    private val events = onWindow + setOf("click", "submit", "input", "change", "load")
    private val modifiers = setOf("once", "debounce", "throttle", "delay", "viewtransition", "passive", "capture", "prevent", "stop", "case")

    override fun models(attribute: DatastarAttribute) =
        attribute.key in events && (if (attribute.key in onWindow) modifiers + "window" else modifiers).containsAll(attribute.modifiers)

    override fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {
        if (attribute.key == "load") page.evaluate(attribute, DatastarDomEvent("load", element))
    }

    override fun handle(event: DatastarDomEvent, element: Element, attribute: DatastarAttribute, page: DatastarPage): Boolean {
        if (attribute.key != event.type || ("window" in attribute.modifiers) != event.window) return false
        page.evaluate(attribute.copy(expression = attribute.expression.replace("(evt.preventDefault(), ", "(")), event)
        return true
    }
}
