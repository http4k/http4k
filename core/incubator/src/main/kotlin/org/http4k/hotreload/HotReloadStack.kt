package org.http4k.hotreload

import org.http4k.core.Filter
import org.http4k.core.then
import org.http4k.filter.CachingFilters.CacheResponse.NoCache

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun HotReloadStack(): Filter {
    val reloadEventSourcePath = "/http4k/hot-reload"

    return NoCache()
        .then(InsertHotReloadScript(reloadEventSourcePath))
        .then(BlockResponse(reloadEventSourcePath))
}

