package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import java.nio.ByteBuffer
import javax.activation.MimetypesFileTypeMap

internal class ResourceLoadingHandler(private val pathSegments: String,
                                      private val resourceLoader: ResourceLoader,
                                      extraPairs: Map<String, ContentType>) : HttpHandler {
    private val extMap = MimetypesFileTypeMap(ContentType::class.java.getResourceAsStream("/META-INF/mime.types"))

    init {
        extMap.addMimeTypes(extraPairs.map { (first, second) -> second.value + "\t\t\t" + first }.joinToString("\n"))
    }

    override fun invoke(p1: Request): Response {
        val path = convertPath(p1.uri.path)
        return resourceLoader.load(path)?.let { url ->
            val lookupType = ContentType(extMap.getContentType(path))
            if (p1.method == GET && lookupType != OCTET_STREAM) {
                Response(OK)
                    .header("Content-Type", lookupType.value)
                    .body(Body(ByteBuffer.wrap(url.openStream().readBytes())))
            } else Response(NOT_FOUND)
        } ?: Response(NOT_FOUND)
    }

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replace(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}

data class StaticRoutingHttpHandler(private val pathSegments: String,
                                    private val resourceLoader: ResourceLoader,
                                    private val extraPairs: Map<String, ContentType>,
                                    private val filter: Filter = Filter.NoOp
) : RoutingHttpHandler {

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = filter.then(new))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(pathSegments = new + pathSegments)

    override fun match(request: Request): HttpHandler? = invoke(request).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }

    private val httpHandler = filter.then(ResourceLoadingHandler(pathSegments, resourceLoader, extraPairs))

    override fun invoke(req: Request): Response = httpHandler(req)
}

internal data class AggregateRoutingHttpHandler(
    private val list: List<RoutingHttpHandler>,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler) : RoutingHttpHandler {

    constructor(vararg list: RoutingHttpHandler) : this(list.toList())

    override fun invoke(request: Request): Response = (match(request) ?: notFoundHandler)(request)

    override fun match(request: Request): HttpHandler? = list.asSequence().mapNotNull { next -> next.match(request) }.firstOrNull()

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(list = list.map { it.withFilter(new) }, notFoundHandler = new.then(notFoundHandler))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(list = list.map { it.withBasePath(new) })
}

internal val routeNotFoundHandler: HttpHandler = { Response(NOT_FOUND.description("Route not found")) }

internal data class TemplateRoutingHttpHandler(
    private val method: Method?,
    private val template: UriTemplate,
    private val httpHandler: HttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler
) : RoutingHttpHandler {

    override fun match(request: Request): HttpHandler? =
        if (template.matches(request.uri.path) && (method == null || method == request.method))
            { r: Request -> RoutedResponse(httpHandler(RoutedRequest(r, template)), template) }
        else null

    override fun invoke(request: Request): Response = (match(request) ?: notFoundHandler)(request)

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(httpHandler = new.then(httpHandler), notFoundHandler = new.then(notFoundHandler))

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
