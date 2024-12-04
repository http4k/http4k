package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
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
) : RoutingHandler<Response, Filter, RoutingHttpHandler>(routes, ::RoutingHttpHandler)

class TemplatedHttpRoute(
    uriTemplate: UriTemplate, handler: HttpHandler, router: Router = All, filter: Filter = Filter.NoOp
) : TemplatedRoute<Response, Filter, TemplatedHttpRoute>(
    uriTemplate = uriTemplate,
    handler = handler,
    router = router,
    filter = filter,
    responseFor = { Response(it) },
    addUriTemplateFilter = { next -> { RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate) } }
) {
    override fun withBasePath(prefix: String) = TemplatedHttpRoute(uriTemplate.prefixed(prefix), handler, router, filter)

    override fun withFilter(new: Filter) = TemplatedHttpRoute(uriTemplate, handler, router, new.then(filter))

    override fun withRouter(other: Router) = TemplatedHttpRoute(uriTemplate, handler, router.and(other), filter)
}

data class SimpleRouteMatcher(
    private val router: Router,
    private val handler: HttpHandler,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter> {

    override fun match(request: Request) = when (val result = router(request)) {
        is Matched -> RoutingMatch(0, filter.then(handler))
        is NotMatched -> RoutingMatch(1, filter.then { _: Request -> Response(result.status) })
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
