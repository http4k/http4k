package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer

internal data class TemplateRoutingWsHandler(private val template: UriTemplate,
                                             private val consumer: WsConsumer) : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = if (template.matches(request.uri.path)) { ws ->
        consumer(object : Websocket by ws {
            override val upgradeRequest: Request = RoutedRequest(ws.upgradeRequest, template)
        })
    } else null

    override fun withBasePath(new: String): TemplateRoutingWsHandler = copy(template = UriTemplate.from("$new/$template"))
}
