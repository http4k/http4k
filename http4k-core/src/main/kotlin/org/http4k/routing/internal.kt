package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.MimeTypes
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RouterMatchResult.*
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer

internal class ResourceLoadingHandler(private val pathSegments: String,
                                      private val resourceLoader: ResourceLoader,
                                      extraFileExtensionToContentTypes: Map<String, ContentType>) : HttpHandler {
    private val extMap = MimeTypes(extraFileExtensionToContentTypes)

    override fun invoke(p1: Request): Response = if (p1.uri.path.startsWith(pathSegments)) {
        val path = convertPath(p1.uri.path)
        resourceLoader.load(path)?.let { url ->
            val lookupType = extMap.forFile(path)
            if (p1.method == GET && lookupType != OCTET_STREAM) {
                Response(OK)
                    .header("Content-Type", lookupType.value)
                    .body(Body(url.openStream()))
            } else Response(NOT_FOUND)
        } ?: Response(NOT_FOUND)
    } else Response(NOT_FOUND)

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replaceFirst(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}

internal data class StaticRoutingHttpHandler(private val pathSegments: String,
                                             private val resourceLoader: ResourceLoader,
                                             private val extraFileExtensionToContentTypes: Map<String, ContentType>,
                                             private val filter: Filter = Filter.NoOp
) : RoutingHttpHandler {

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = new.then(filter))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(pathSegments = new + pathSegments)

    private val handlerNoFilter = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)
    private val handlerWithFilter = filter.then(handlerNoFilter)

    override fun match(request: Request): RouterMatchResult = handlerNoFilter(request).let {
        if (it.status != NOT_FOUND) MatchingHandler(filter.then { _: Request -> it }) else null
    } ?: Unmatched

    override fun invoke(request: Request): Response = handlerWithFilter(request)
}

internal data class AggregateRoutingHttpHandler(
    private val list: List<RoutingHttpHandler>,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotMatchedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    constructor(vararg list: RoutingHttpHandler) : this(list.toList())

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        is MethodNotMatched -> methodNotMatchedHandler(request)
        is Unmatched -> notFoundHandler(request)
    }

    override fun match(request: Request): RouterMatchResult = list.asSequence()
        .map { next -> next.match(request) }
        .sorted()
        .firstOrNull() ?: Unmatched

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(list = list.map { it.withFilter(new) }, notFoundHandler = new.then(notFoundHandler), methodNotMatchedHandler = new.then(methodNotMatchedHandler))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(list = list.map { it.withBasePath(new) })
}

internal val routeNotFoundHandler: HttpHandler = { Response(NOT_FOUND.description("Route not found")) }

internal val routeMethodNotAllowedHandler: HttpHandler = { Response(METHOD_NOT_ALLOWED.description("Method not allowed")) }

internal data class TemplateRoutingHttpHandler(
    private val method: Method?,
    private val template: UriTemplate,
    private val httpHandler: HttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request): RouterMatchResult =
        if (template.matches(request.uri.path)) {
            when (method) {
                null, request.method -> MatchingHandler { RoutedResponse(httpHandler(RoutedRequest(it, template)), template) }
                else -> MethodNotMatched
            }
        } else Unmatched

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        is MethodNotMatched -> methodNotAllowedHandler(request)
        is Unmatched -> notFoundHandler(request)
    }

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(httpHandler = new.then(httpHandler), notFoundHandler = new.then(notFoundHandler), methodNotAllowedHandler = new.then(methodNotAllowedHandler))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(template = UriTemplate.from("$new/$template"))
}

internal data class TemplateRoutingWsHandler(private val template: UriTemplate,
                                             private val consumer: WsConsumer) : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = if (template.matches(request.uri.path)) { ws ->
        consumer(object : Websocket by ws {
            override val upgradeRequest: Request = RoutedRequest(ws.upgradeRequest, template)
        })
    } else null

    override fun withBasePath(new: String): TemplateRoutingWsHandler = copy(template = UriTemplate.from("$new/$template"))
}

internal data class SinglePageAppRoutingHandler(
    private val pathSegments: String,
    private val staticHandler: StaticRoutingHttpHandler
) : RoutingHttpHandler {

    override fun invoke(request: Request): Response {
        val matchOnStatic = when(val matchResult = staticHandler.match(request)) {
            is MatchingHandler -> matchResult(request)
            else -> null
        }

        val matchOnIndex = when (val matchResult = staticHandler.match(Request(GET, pathSegments))) {
            is MatchingHandler -> matchResult.httpHandler
            else -> null
        }

        val fallbackHandler = matchOnIndex ?: { Response(NOT_FOUND) }
        return matchOnStatic ?: fallbackHandler(Request(GET, pathSegments))
    }

    override fun match(request: Request) = MatchingHandler(this)

    override fun withFilter(new: Filter) = copy(staticHandler = staticHandler.withFilter(new) as StaticRoutingHttpHandler)

    override fun withBasePath(new: String) = SinglePageAppRoutingHandler(new + pathSegments, staticHandler.withBasePath(new) as StaticRoutingHttpHandler)
}
