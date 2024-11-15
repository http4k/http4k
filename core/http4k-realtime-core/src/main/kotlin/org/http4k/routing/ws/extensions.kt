package org.http4k.routing.ws

import org.http4k.core.UriTemplate
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.TemplateRoutingWsHandler
import org.http4k.websocket.WsHandler

infix fun String.bind(ws: WsHandler): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), ws)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)
