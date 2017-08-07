package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
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

class StaticRoutingHttpHandler constructor(private val httpHandler: StaticRoutingHttpHandler.Companion.Handler) : RoutingHttpHandler {
    override fun withFilter(new: Filter): RoutingHttpHandler = StaticRoutingHttpHandler(httpHandler.copy(filter = httpHandler.filter.then(new)))

    override fun withBasePath(new: String): RoutingHttpHandler = StaticRoutingHttpHandler(httpHandler.copy(pathSegments = new + httpHandler.pathSegments))

    override fun match(request: Request): HttpHandler? = invoke(request).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }

    override fun invoke(req: Request): Response = httpHandler(req)

    companion object {
        data class Handler(val pathSegments: String,
                           private val resourceLoader: ResourceLoader,
                           private val extraPairs: Map<String, ContentType>,
                           val filter: Filter = Filter { next -> { next(it) } }
        ) : HttpHandler {
            private val extMap = MimetypesFileTypeMap(ContentType::class.java.getResourceAsStream("/META-INF/mime.types"))

            init {
                extMap.addMimeTypes(extraPairs
                    .map { (first, second) -> second.value + "\t\t\t" + first }.joinToString("\n")
                )
            }

            private val handler = filter.then { req ->
                val path = convertPath(req.uri.path)
                resourceLoader.load(path)?.let { url ->
                    val lookupType = ContentType(extMap.getContentType(path))
                    if (req.method == GET && lookupType != OCTET_STREAM) {
                        Response(OK)
                            .header("Content-Type", lookupType.value)
                            .body(Body(ByteBuffer.wrap(url.openStream().readBytes())))
                    } else Response(NOT_FOUND)
                } ?: Response(NOT_FOUND)

            }

            override fun invoke(req: Request): Response = handler(req)

            private fun convertPath(path: String): String {
                val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replace(pathSegments, "")
                val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
                return resolved.replaceFirst("/", "")
            }
        }

    }
}

internal class GroupRoutingHttpHandler(private val httpHandler: GroupRoutingHttpHandler.Companion.Handler) : RoutingHttpHandler {
    override fun withFilter(new: Filter): RoutingHttpHandler = GroupRoutingHttpHandler(
        httpHandler.copy(route = httpHandler.route.copy(handler = new.then(httpHandler.route.handler))))

    override fun withBasePath(new: String): RoutingHttpHandler = GroupRoutingHttpHandler(
        httpHandler.copy(pathSegments = UriTemplate.from(new + httpHandler.pathSegments?.toString().orEmpty()),
            route = httpHandler.route.copy(template = UriTemplate.from("$new/${httpHandler.route.template}"))
        )
    )

    override fun invoke(request: Request): Response = httpHandler(request)

    override fun match(request: Request): HttpHandler? = httpHandler.match(request)

    companion object {
        internal data class Handler(internal val pathSegments: UriTemplate? = null, internal val route: Route) : HttpHandler {
            private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(NOT_FOUND.description("Route not found")) }

            fun match(request: Request): HttpHandler? = if (pathSegments?.matches(request.uri.path) != false)
                route.match(request)
            else null

            override fun invoke(request: Request): Response = handler(request)
        }
    }
}

internal fun Request.uriTemplate(): UriTemplate = headers.findSingle("x-uri-template")?.let { UriTemplate.from(it) } ?: throw IllegalStateException("x-uri-template header not present in the request")