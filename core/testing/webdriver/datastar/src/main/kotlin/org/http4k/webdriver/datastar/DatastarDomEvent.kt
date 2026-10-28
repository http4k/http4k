package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

/**
 * An event the driver produces: [type] on [target], with the [key] for keyboard events. A
 * [window] event is offered to every element, for attributes listening on the window.
 */
data class DatastarDomEvent(val type: String, val target: Element, val key: String? = null, val window: Boolean = false)
