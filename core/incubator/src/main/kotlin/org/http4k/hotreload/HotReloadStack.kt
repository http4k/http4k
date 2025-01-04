package org.http4k.hotreload

import org.http4k.core.then
import org.http4k.filter.CachingFilters.CacheResponse.NoCache

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun HotReloadStack() = NoCache()
    .then(HandleInitialPing("/http4k/ping"))
    .then(InsertHotReloadScript("/http4k/hot-reload"))
    .then(BlockResponse("/http4k/hot-reload"))

