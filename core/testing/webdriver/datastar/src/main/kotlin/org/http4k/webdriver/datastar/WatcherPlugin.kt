package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

/**
 * Applies what a datastar-patch-elements event carries, beyond morphing it in: told about every
 * element the patch carries.
 */
fun interface WatcherPlugin : DatastarPlugin {
    fun patched(element: Element, page: DatastarPage)
}
