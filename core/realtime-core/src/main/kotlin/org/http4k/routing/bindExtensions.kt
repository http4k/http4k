package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.routing.sse.bind
import org.http4k.routing.websocket.bind
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

infix fun String.bindHttp(action: HttpHandler): RoutingHttpHandler = this bind action
infix fun String.bindHttp(action: RoutingHttpHandler): RoutingHttpHandler = this bind action
infix fun String.bindHttp(method: Method): PathMethod = PathMethod(this, method)

infix fun String.bindSse(action: SseHandler): RoutingSseHandler = this bind action
infix fun String.bindSse(action: RoutingSseHandler): RoutingSseHandler = this bind action
infix fun String.bindSse(method: Method): PathMethod = PathMethod(this, method)
infix fun Router.bindSse(handler: RoutingSseHandler): RoutingSseHandler = this bind handler
infix fun Router.bindSse(handler: SseHandler): RoutingSseHandler = this bind handler

infix fun String.bindWs(action: WsHandler): RoutingWsHandler = this bind action
infix fun String.bindWs(action: RoutingWsHandler): RoutingWsHandler = this bind action
infix fun Router.bindWs(handler: RoutingWsHandler): RoutingWsHandler = this bind handler
infix fun Router.bindWs(handler: WsHandler): RoutingWsHandler = this bind handler
