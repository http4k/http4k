package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched

fun routes(vararg list: Pair<Method, HttpHandler>) =
    routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler) = routes(list.toList())

fun routes(routers: List<RoutingHttpHandler>) = RoutingHttpHandler(routers.flatMap { it.routes })

class RoutingHttpHandler(
    routes: List<RouteMatcher<Response, Filter>>
) : RoutingHandler<Response, Filter, RoutingHttpHandler>(routes, Response(NOT_FOUND), ::RoutingHttpHandler)

data class TemplatedHttpRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val router: Router = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter> {

    init {
        require(handler !is RoutingHandler<*, *, *>)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = router(request)) {
            is Matched -> RoutingMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is NotMatched -> RoutingMatchResult(1, filter.then { _: Request -> Response(result.status) })
        }

        else -> RoutingMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    override fun withRouter(other: Router) = copy(router = router.and(other))

    override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = copy(filter = new.then(filter))

    override fun toString() = "template=$uriTemplate AND ${router.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

data class SimpleRouteMatcher(
    private val router: Router,
    private val handler: HttpHandler,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter> {

    override fun match(request: Request) = when (val result = router(request)) {
        is Matched -> RoutingMatchResult(0, filter.then(handler))
        is NotMatched -> RoutingMatchResult(1, filter.then { _: Request -> Response(result.status) })
    }

    override fun withBasePath(prefix: String): RouteMatcher<Response, Filter> =
        TemplatedHttpRoute(UriTemplate.from(prefix), handler, router, filter)

    override fun withRouter(other: Router): RouteMatcher<Response, Filter> = copy(router = router.and(other))

    override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = copy(filter = new.then(filter))
}

data class HttpPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) = when (handler) {
        is RoutingHttpHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
        else -> RoutingHttpHandler(listOf(TemplatedHttpRoute(UriTemplate.from(path), handler, method.asRouter())))
    }
}
