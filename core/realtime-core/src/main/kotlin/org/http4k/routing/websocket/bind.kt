package org.http4k.routing.websocket

import org.http4k.core.UriTemplate
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.TemplatedWsRoute
import org.http4k.websocket.WsHandler

infix fun String.bind(action: WsHandler) =
    RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(this), action)))

infix fun String.bind(handler: RoutingWsHandler) = handler.withBasePath(this)
