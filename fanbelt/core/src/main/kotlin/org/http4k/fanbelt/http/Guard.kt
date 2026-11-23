package org.http4k.fanbelt.http

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.routing.thenPoly
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Admits or refuses requests to a page and its stream: admit returns the request to continue with, or null to refuse.
 * Refused requests are answered by onRefused; a refused stream gets only its status and headers. WebSockets are not
 * guarded.
 */
internal fun Guard(onRefused: HttpHandler, admit: (Request) -> Request?) = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { Filter { http -> { admit(it)?.let(http) ?: onRefused(it) } }.thenPoly(it) },
        sse = next.sse
            ?.let {
                SseFilter { sse ->
                    {
                        admit(it)?.let(sse) ?: run {
                            val response = onRefused(it)
                            SseResponse(response.status, response.headers) { it.close() }
                        }
                    }
                }.thenPoly(it)
            },
        ws = next.ws,
    )
}
