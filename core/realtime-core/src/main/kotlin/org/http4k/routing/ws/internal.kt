package org.http4k.routing.ws

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.Any
import org.http4k.routing.Predicate
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched
import org.http4k.routing.RoutedRequest
import org.http4k.routing.and
import org.http4k.routing.asPredicate
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
import org.http4k.websocket.then

data class RoutingWsHandler(
    val routes: List<TemplatedWsRoute>,
    private val filter: WsFilter = WsFilter.NoOp
) : WsHandler {
    override fun invoke(request: Request) = filter.then(routes
        .map { it.match(request) }
        .sortedBy(WsMatchResult::priority)
        .first().handler)(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: WsFilter) = copy(filter = new.then(filter))

    fun withPredicate(predicate: Predicate) =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString() = routes.sortedBy(TemplatedWsRoute::toString).joinToString("\n")
}

data class WsPathMethod(val path: String, val method: Method) {
    infix fun to(handler: WsHandler) = when (handler) {
        is RoutingWsHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}

data class TemplatedWsRoute(
    private val uriTemplate: UriTemplate,
    private val handler: WsHandler,
    private val predicate: Predicate = Any
) {
    init {
        require(handler !is RoutingWsHandler)
    }

    internal fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = predicate(request)) {
            is Matched -> WsMatchResult(0, AddUriTemplate(uriTemplate).then(handler))
            is NotMatched -> WsMatchResult(1) { _: Request -> WsResponse { it.close(REFUSE) } }
        }

        else -> WsMatchResult(2) { _: Request -> WsResponse() { it.close(REFUSE) } }
    }

    fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = WsFilter { next ->
        {
            next(RoutedRequest(it, uriTemplate))
        }
    }
}

internal data class WsMatchResult(val priority: Int, val handler: WsHandler)
