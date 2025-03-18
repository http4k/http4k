package org.http4k.routing.websocket

import org.http4k.core.UriTemplate
import org.http4k.routing.Router
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.SimpleSseRouteMatcher
import org.http4k.routing.SimpleWsRouteMatcher
import org.http4k.routing.TemplatedWsRoute
import org.http4k.websocket.WsHandler

infix fun String.bind(action: WsHandler) =
    RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(this), action)))
infix fun String.bind(handler: RoutingWsHandler) = handler.withBasePath(this)
infix fun Router.bind(handler: RoutingWsHandler) = handler.withRouter(this)
infix fun Router.bind(handler: WsHandler): RoutingWsHandler =
    RoutingWsHandler(listOf(SimpleWsRouteMatcher(this, handler)))
