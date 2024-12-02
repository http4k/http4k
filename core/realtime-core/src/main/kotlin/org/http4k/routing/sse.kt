package org.http4k.routing

import org.http4k.core.Method
import org.http4k.routing.sse.RoutingSseHandler
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse

fun sse(sse: SseConsumer): SseHandler = { SseResponse(sse) }

fun sse(vararg methods: Pair<Method, SseHandler>): SseHandler = {
    methods.toMap()[it.method]?.invoke(it) ?: SseResponse { it.close() }
}

fun sse(vararg list: RoutingSseHandler) = sse(list.toList())

fun sse(routers: List<RoutingSseHandler>) =
    RoutingSseHandler(routers.flatMap { it.routes })
