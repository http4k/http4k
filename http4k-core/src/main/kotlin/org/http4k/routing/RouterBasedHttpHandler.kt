package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

internal data class RouterBasedHttpHandler(
    private val router: Router,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request): RouterMatch = router.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult
        is MethodNotMatched -> methodNotAllowedHandler
        else -> notFoundHandler
    }(request)

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(
        router.withFilter(new),
        notFoundHandler = new.then(notFoundHandler),
        methodNotAllowedHandler = new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler = copy(router = router.withBasePath(new))
}

internal data class Prefix(private val template: String) : Router {
    override fun match(request: Request) = when {
        UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> MatchedWithoutHandler
        else -> Unmatched
    }

    override fun withBasePath(new: String) = Prefix("$new/${template.trimStart('/')}")
}

internal val routeNotFoundHandler: HttpHandler = { Response(NOT_FOUND.description("Route not found")) }

internal val routeMethodNotAllowedHandler: HttpHandler = { Response(METHOD_NOT_ALLOWED.description("Method not allowed")) }

internal data class TemplateRouter(private val template: UriTemplate,
                                   private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request) = when {
        template.matches(request.uri.path) ->
            MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
        else -> Unmatched
    }

    override fun withBasePath(new: String): Router =
        TemplateRouter(UriTemplate.from("$new/${template}"),
        when (httpHandler) {
            is RoutingHttpHandler -> httpHandler.withBasePath(new)
            else -> httpHandler
        })

    override fun withFilter(new: Filter): Router = copy(httpHandler = when (httpHandler) {
        is RoutingHttpHandler -> httpHandler.withFilter(new)
        else -> new.then(httpHandler)
    })
}
