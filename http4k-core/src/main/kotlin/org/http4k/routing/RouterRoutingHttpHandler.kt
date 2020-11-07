package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

internal data class RouterRoutingHttpHandler(
    private val router: Router,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request): RouterMatch = router.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult
        is MethodNotMatched -> methodNotAllowedHandler
        is Unmatched -> notFoundHandler
        is MatchedWithoutHandler -> notFoundHandler
    }(request)

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(
        router.withFilter(new),
        notFoundHandler = new.then(notFoundHandler),
        methodNotAllowedHandler = new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler = copy(router = router.withBasePath(new))
}

data class Prefix(private val template: String) : Router {
    override fun match(request: Request) =
        when {
            UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> MatchedWithoutHandler
            else -> Unmatched
        }

    override fun withBasePath(new: String) = Prefix("$new/${template.trimStart('/')}")
}

data class TemplatingRouter(private val method: Method?,
                            private val template: UriTemplate,
                            private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request): RouterMatch = if (template.matches(request.uri.path)) {
        when (method) {
            null, request.method -> MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
            else -> MethodNotMatched
        }
    } else Unmatched

    override fun withBasePath(new: String): Router = copy(
        template = UriTemplate.from("$new/${template}")
    )

    override fun withFilter(new: Filter): Router = copy(httpHandler = when (httpHandler) {
        is RoutingHttpHandler -> httpHandler.withFilter(new)
        else -> new.then(httpHandler)
    })
}
