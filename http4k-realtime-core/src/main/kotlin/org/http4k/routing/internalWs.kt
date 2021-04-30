package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsStatus

sealed class WsRouterMatch(private val priority: Int) :
    Comparable<WsRouterMatch> {

    data class MatchingHandler(private val wsHandler: WsConsumer) : WsRouterMatch(0), WsConsumer by wsHandler

    object Unmatched : WsRouterMatch(1)

    override fun compareTo(other: WsRouterMatch): Int = priority.compareTo(other.priority)
}

internal class RouterWsHandler(private val list: List<WsRouter>) : RoutingWsHandler {
    override fun match(request: Request) = list.map { next -> next.match(request) }.minOrNull() ?: WsRouterMatch.Unmatched

    override operator fun invoke(request: Request): WsConsumer = when (val match = match(request)) {
        is WsRouterMatch.MatchingHandler -> match
        is WsRouterMatch.Unmatched -> { it: Websocket -> it.close(WsStatus.REFUSE) }
    }

    override fun withBasePath(new: String): RoutingWsHandler =
        websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}

internal class TemplateRoutingWsHandler(
    private val template: UriTemplate,
    private val consumer: WsConsumer
) : RoutingWsHandler {
    override fun match(request: Request): WsRouterMatch = when {
        template.matches(request.uri.path) -> WsRouterMatch.MatchingHandler { ws ->
            consumer(object : Websocket by ws {
                override val upgradeRequest: Request = RoutedRequest(ws.upgradeRequest, template)
            })
        }
        else -> WsRouterMatch.Unmatched
    }

    override operator fun invoke(request: Request): WsConsumer = when (val match = match(request)) {
        is WsRouterMatch.MatchingHandler -> match
        is WsRouterMatch.Unmatched -> { it: Websocket -> it.close(WsStatus.REFUSE) }
    }

    override fun withBasePath(new: String): TemplateRoutingWsHandler =
        TemplateRoutingWsHandler(UriTemplate.from("$new/$template"), consumer)
}
