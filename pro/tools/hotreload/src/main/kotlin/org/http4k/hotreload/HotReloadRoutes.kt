package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CachingFilters.CacheResponse.NoCache
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes
import java.time.Duration

/**
 * Filter which injects a script into HTML responses using an event source to detect changes and reload the page.
 */
fun HotReloadRoutes(app: HttpHandler, sleeper: (Duration) -> Unit = Thread::sleep) = NoCache()
    .then(InsertHotReloadScript("/http4k/hot-reload"))
    .then(routes(
        "http4k" bind routes(
            "/ping" bind { Response(OK).body("pong") },
            "/hot-reload" bind {
                runCatching { sleeper(Duration.ofMinutes(10)) }
                    .map { Response(OK) }
                    .getOrDefault(Response(I_M_A_TEAPOT))
            }
        ),
        orElse bind app
    ))


