package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.WsRouterMatch.MatchingHandler
import org.http4k.routing.WsRouterMatch.Unmatched
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
import org.http4k.websocket.then

sealed class WsRouterMatch(private val priority: Int) :
    Comparable<WsRouterMatch> {

    data class MatchingHandler(private val handler: WsHandler) : WsRouterMatch(0), WsHandler by handler

    object Unmatched : WsRouterMatch(1)

    override fun compareTo(other: WsRouterMatch): Int = priority.compareTo(other.priority)
}

internal class RouterWsHandler(private val list: List<WsRouter>) : RoutingWsHandler {
    override fun match(request: Request) = list.minOfOrNull { it.match(request) } ?: Unmatched

    override operator fun invoke(request: Request): WsResponse = when (val match = match(request)) {
        is MatchingHandler -> match(request)
        is Unmatched -> WsResponse { it.close() }
    }

    override fun withBasePath(new: String): RoutingWsHandler =
        websockets(*list.map { it.withBasePath(new) }.toTypedArray())

    override fun withFilter(new: WsFilter) = RouterWsHandler(list.map { it.withFilter(new) })
}

internal class TemplateRoutingWsHandler(
    private val template: UriTemplate,
    private val handler: WsHandler
) : RoutingWsHandler {
    override fun match(request: Request): WsRouterMatch = when {
        template.matches(request.uri.path) -> MatchingHandler { req ->
            handler(
                RoutedRequest(
                    req,
                    template
                )
            )
        }

        else -> Unmatched
    }

    override operator fun invoke(request: Request): WsResponse = when (val matched = match(request)) {
        is MatchingHandler -> matched(RoutedRequest(request, template))
        is Unmatched -> WsResponse { it.close(REFUSE) }
    }

    override fun withBasePath(new: String): TemplateRoutingWsHandler =
        TemplateRoutingWsHandler(UriTemplate.from("$new/$template"), handler)

    override fun withFilter(new: WsFilter) = TemplateRoutingWsHandler(template, new.then(handler))
}
