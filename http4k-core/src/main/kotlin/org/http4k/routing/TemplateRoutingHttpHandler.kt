package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.then

internal data class TemplateRoutingHttpHandler(
    private val method: Method?,
    private val template: UriTemplate,
    private val httpHandler: HttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request): RouterMatch =
        if (template.matches(request.uri.path)) {
            when (method) {
                null, request.method -> RouterMatch.MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
                else -> RouterMatch.MethodNotMatched
            }
        } else RouterMatch.Unmatched

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        is RouterMatch.MethodNotMatched -> methodNotAllowedHandler(request)
        is RouterMatch.Unmatched -> notFoundHandler(request)
        RouterMatch.Matched -> TODO()
    }

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(httpHandler = new.then(httpHandler),
            notFoundHandler = new.then(notFoundHandler),
            methodNotAllowedHandler = new.then(methodNotAllowedHandler)
        )

    override fun withBasePath(new: String): RoutingHttpHandler = copy(template = UriTemplate.from("$new/$template"))
}
