package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler =
    TemplateRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

fun websockets(ws: WsConsumer): WsHandler = { ws }

fun websockets(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}

internal data class TemplateRoutingWsHandler(
    private val template: UriTemplate,
    private val consumer: WsConsumer
) : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = if (template.matches(request.uri.path)) { ws ->
        consumer(object : Websocket by ws {
            override val upgradeRequest: Request = RoutedRequest(ws.upgradeRequest, template)
        })
    } else null

    override fun withBasePath(new: String): TemplateRoutingWsHandler = copy(template = UriTemplate.from("$new/$template"))
}
