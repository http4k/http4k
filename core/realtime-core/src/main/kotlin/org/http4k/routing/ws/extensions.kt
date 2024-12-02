package org.http4k.routing.ws

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.websocket.WsHandler

infix fun String.bind(method: Method) = WsPathMethod(this, method)
infix fun String.bind(handler: RoutingWsHandler) = handler.withBasePath(this)
infix fun String.bind(action: WsHandler) =
    RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(this), action)))

