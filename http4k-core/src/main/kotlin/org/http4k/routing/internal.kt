package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.core.findSingle
import org.http4k.core.then
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

internal class StaticRoutingHttpHandler(private val pathSegments: String,
                                        private val resourceLoader: ResourceLoader,
                                        private val extraPairs: Map<String, ContentType>,
                                        private val filter: Filter = Filter { next -> { next(it) } }
) : RoutingHttpHandler {

    override fun withFilter(new: Filter): RoutingHttpHandler = StaticRoutingHttpHandler(pathSegments, resourceLoader, extraPairs, filter.then(new))

    override fun withBasePath(new: String): RoutingHttpHandler = StaticRoutingHttpHandler(new + pathSegments, resourceLoader, extraPairs, filter)

    override fun match(request: Request): HttpHandler? = invoke(request).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }

    private val handler = filter.then(ResourceLoadingHandler(pathSegments, resourceLoader, extraPairs))

    override fun invoke(req: Request): Response = handler(req)
}

private fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = header("x-uri-template", uriTemplate.toString())

internal class TemplateRoutingHttpHandler(private val method: Method?,
                                          private val template: UriTemplate,
                                          private val handler: HttpHandler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? =
        if (template.matches(request.uri.path) && (method == null || method == request.method))
            { r: Request -> handler(r.withUriTemplate(template)) }
        else null

    override fun invoke(request: Request): Response =
        match(request)?.invoke(request) ?: Response(NOT_FOUND.description("Route not found"))

    override fun withFilter(new: Filter): RoutingHttpHandler = TemplateRoutingHttpHandler(method, template, new.then(handler))

    override fun withBasePath(new: String): RoutingHttpHandler = TemplateRoutingHttpHandler(method, UriTemplate.from("$new/$template"), handler)
}

internal fun Request.uriTemplate(): UriTemplate = headers.findSingle("x-uri-template")?.let { UriTemplate.from(it) } ?: throw IllegalStateException("x-uri-template header not present in the request")

internal class PredicatingRoutingHttpHandler(private val predicate: RequestPredicate,
                                             private val handler: HttpHandler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = if (predicate(request)) handler else null

    override fun invoke(request: Request): Response =
        match(request)?.invoke(request) ?: Response(NOT_FOUND.description("Route not found"))

    override fun withFilter(new: Filter): RoutingHttpHandler = PredicatingRoutingHttpHandler(predicate, new.then(handler))

    override fun withBasePath(new: String): RoutingHttpHandler = routes(new bind this)
}

