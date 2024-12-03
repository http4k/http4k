package org.http4k.routing.websocket

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.All
import org.http4k.routing.Predicate
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedWsResponse
import org.http4k.routing.and
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.http4k.websocket.then

data class TemplatedWsRoute(
    private val uriTemplate: UriTemplate,
    private val handler: WsHandler,
    private val predicate: Predicate = All
) {
    init {
        require(handler !is RoutingWsHandler)
    }

    internal fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (predicate(request)) {
            is Matched -> WsMatchResult(0, AddUriTemplate(uriTemplate).then(handler))
            is NotMatched -> notMachResult
        }

        else -> notMachResult
    }

    fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = WsFilter { next ->
        {
            RoutedWsResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }

    companion object {
        internal val notMachResult = WsMatchResult(1) { _: Request -> WsResponse { it.close(WsStatus.REFUSE) } }
    }
}
