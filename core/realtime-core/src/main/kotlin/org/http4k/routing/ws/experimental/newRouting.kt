package org.http4k.routing.ws.experimental

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.websocket.WsHandler

fun newWsRoutes(vararg list: Pair<Method, WsHandler>) =
    newWsRoutes(*list.map { "" newWsBind it.first to it.second }.toTypedArray())

fun newWsRoutes(vararg list: RoutedWsHandler) = newWsRoutes(list.toList())

fun newWsRoutes(routers: List<RoutedWsHandler>) = RoutedWsHandler(routers.flatMap { it.routes })

infix fun String.newWsBind(method: Method) = WsPathMethod(this, method)
infix fun String.newWsBind(sseHandler: RoutedWsHandler) = sseHandler.withBasePath(this)
infix fun String.newWsBind(action: WsHandler) =
    RoutedWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(this), action)))
