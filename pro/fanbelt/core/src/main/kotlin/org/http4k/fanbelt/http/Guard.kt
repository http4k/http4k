/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.http

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.routing.thenPoly
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

fun Guard(onRefused: HttpHandler, admit: (Request) -> Request?) = PolyFilter { next ->
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
