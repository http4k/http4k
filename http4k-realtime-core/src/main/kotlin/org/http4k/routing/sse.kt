package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsFilter

interface SseRouter {
    fun match(request: Request): SseRouterMatch
    fun withBasePath(new: String): SseRouter
    fun withFilter(new: SseFilter): SseRouter
}

interface RoutingSseHandler : SseHandler, SseRouter {
    override fun withBasePath(new: String): RoutingSseHandler
    override fun withFilter(new: SseFilter): RoutingSseHandler
}

infix fun String.bind(consumer: SseConsumer): RoutingSseHandler =
    TemplateRoutingSseHandler(UriTemplate.from(this), consumer)

infix fun String.bind(sseHandler: RoutingSseHandler): RoutingSseHandler = sseHandler.withBasePath(this)

fun sse(sse: SseConsumer): SseHandler = { sse }

fun sse(vararg list: SseRouter): RoutingSseHandler = RouterSseHandler(list.toList())
