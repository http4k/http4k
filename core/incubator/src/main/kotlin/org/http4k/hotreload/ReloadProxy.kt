package org.http4k.hotreload

import org.http4k.core.Filter
import org.http4k.core.then

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun ReloadProxy(): Filter {
    val reloadEventSourcePath = "/http4k/hot-reload"

    return InsertHotReloadScript(reloadEventSourcePath)
        .then(BlockConnection(reloadEventSourcePath))
}

