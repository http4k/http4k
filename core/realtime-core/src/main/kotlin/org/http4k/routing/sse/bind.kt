package org.http4k.routing.sse


import org.http4k.core.UriTemplate
import org.http4k.routing.Router
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.SimpleSseRouteMatcher
import org.http4k.routing.TemplatedSseRoute
import org.http4k.sse.SseHandler

infix fun String.bind(sseHandler: RoutingSseHandler) = sseHandler.withBasePath(this)
infix fun String.bind(action: SseHandler) =
    RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(this), action)))
infix fun Router.bind(handler: RoutingSseHandler) = handler.withRouter(this)
infix fun Router.bind(handler: SseHandler): RoutingSseHandler =
    RoutingSseHandler(listOf(SimpleSseRouteMatcher(this, handler)))

