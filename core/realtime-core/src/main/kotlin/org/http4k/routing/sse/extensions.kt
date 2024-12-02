package org.http4k.routing.sse


import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.sse.SseHandler



infix fun String.bind(method: Method) = SsePathMethod(this, method)
infix fun String.bind(sseHandler: RoutingSseHandler) = sseHandler.withBasePath(this)
infix fun String.bind(action: SseHandler) =
    RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(this), action)))
