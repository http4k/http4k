package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.then

internal data class RouterRoutingHttpHandler(
    private val router: Router,
    private val filter: Filter = Filter.NoOp,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request): RouterMatch = when (val matchResult = router.match(request)) {
        is RouterMatch.MatchingHandler -> RouterMatch.MatchingHandler(filter.then(matchResult))
        else -> matchResult
    }

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        is RouterMatch.MethodNotMatched -> filter.then(methodNotAllowedHandler)(request)
        is RouterMatch.Unmatched -> filter.then(notFoundHandler)(request)
        is RouterMatch.MatchedWithoutHandler -> filter.then(notFoundHandler)(request)
    }

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = new.then(filter))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(router = router.withBasePath(new))
}

data class Prefix(private val template: String) : Router {

    override fun match(request: Request) =
        when {
            UriTemplate.from("$template{match:.*}").matches(request.uri.path) -> RouterMatch.MatchedWithoutHandler
            else -> RouterMatch.Unmatched
        }

    override fun withBasePath(new: String) = Prefix("$new/${template}")
}

data class TemplatingRouter(private val method: Method?,
                            private val template: UriTemplate,
                            private val httpHandler: HttpHandler) : Router {
    override fun match(request: Request): RouterMatch = if (template.matches(request.uri.path)) {
        when (method) {
            null, request.method -> RouterMatch.MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
            else -> RouterMatch.MethodNotMatched
        }
    } else RouterMatch.Unmatched

    override fun withBasePath(new: String): Router = copy(
        template = UriTemplate.from("$new/${template}")
    )

    override fun withFilter(new: Filter): Router = copy(httpHandler = when (httpHandler) {
        is RoutingHttpHandler -> httpHandler.withFilter(new)
        else -> new.then(httpHandler)
    })
}
