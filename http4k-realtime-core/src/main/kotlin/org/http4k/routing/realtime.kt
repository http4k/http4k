package org.http4k.routing

import org.http4k.core.UriTemplate
import org.http4k.sse.SseConsumer
import org.http4k.websocket.WsConsumer

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler =
    TemplateRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

infix fun String.bind(consumer: SseConsumer): RoutingSseHandler =
    TemplateRoutingSseHandler(UriTemplate.from(this), consumer)

infix fun String.bind(sseHandler: RoutingSseHandler): RoutingSseHandler = sseHandler.withBasePath(this)
