package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

/**
 * Models a data-[name] attribute: set up once when an element carrying it appears, re-applied on
 * every reactive pass, and offered the events the driver produces.
 */
interface AttributePlugin : DatastarPlugin {
    val name: String

    /** Whether this plugin models the attribute as written (its key and modifiers). */
    fun models(attribute: DatastarAttribute): Boolean = true

    fun initialise(element: Element, attribute: DatastarAttribute, page: DatastarPage) {}

    /** Re-applies the attribute from the signals. Must not dispatch actions. */
    fun render(element: Element, attribute: DatastarAttribute, page: DatastarPage) {}

    /** Reacts to an event on the element. Returns true if the event was handled, suppressing the browser's default. */
    fun handle(event: DatastarDomEvent, element: Element, attribute: DatastarAttribute, page: DatastarPage): Boolean = false
}
