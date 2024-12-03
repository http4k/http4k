package org.http4k.routing.sse

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.routing.asPredicate
import org.http4k.sse.SseHandler

data class SsePathMethod(val path: String, val method: Method) {
    infix fun to(handler: SseHandler) = when (handler) {
        is RoutingSseHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}
