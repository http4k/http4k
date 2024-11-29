package org.http4k.routing.sse.experimental

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.sse.SseHandler

fun newSseRoutes(vararg list: Pair<Method, SseHandler>) =
    newSseRoutes(*list.map { "" newSseBind it.first to it.second }.toTypedArray())

fun newSseRoutes(vararg list: RoutedSseHandler) = newSseRoutes(list.toList())

fun newSseRoutes(routers: List<RoutedSseHandler>) = RoutedSseHandler(routers.flatMap { it.routes })

infix fun String.newSseBind(method: Method) = SsePathMethod(this, method)
infix fun String.newSseBind(sseHandler: RoutedSseHandler) = sseHandler.withBasePath(this)
infix fun String.newSseBind(action: SseHandler) =
    RoutedSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(this), action)))
