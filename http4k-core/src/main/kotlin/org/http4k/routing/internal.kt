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

class StaticRouter constructor(private val basePath: String,
                               private val resourceLoader: ResourceLoader,
                               private val extraPairs: Map<String, ContentType>,
                               private val filter: Filter?) : RoutingHttpHandler {

    override fun withBasePath(basePath: String): RoutingHttpHandler = StaticRouter(basePath + this.basePath, resourceLoader, extraPairs, filter)

    override fun match(request: Request): HttpHandler? = invoke(request).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }

    private val extMap = MimetypesFileTypeMap(ContentType::class.java.getResourceAsStream("/META-INF/mime.types"))

    init {
        extMap.addMimeTypes(extraPairs
            .map { (first, second) -> second.value + "\t\t\t" + first }.joinToString("\n")
        )
    }

    override fun invoke(req: Request): Response {
        val handler: HttpHandler = {
            val path = convertPath(req.uri.path)
            resourceLoader.load(path)?.let {
                url ->
                val lookupType = ContentType(extMap.getContentType(path))
                if (req.method == GET && lookupType != OCTET_STREAM) {
                    Response(OK)
                        .header("Content-Type", lookupType.value)
                        .body(Body(ByteBuffer.wrap(url.openStream().readBytes())))
                } else Response(NOT_FOUND)
            } ?: Response(NOT_FOUND)

        }
        return (filter?.then(handler) ?: handler).invoke(req)
    }

    private fun convertPath(path: String): String {
        val newPath = if (basePath == "/" || basePath == "") path else path.replace(basePath, "")
        val resolved = if (newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}

internal data class GroupRoutingHttpHandler(private val basePath: UriTemplate? = null, private val routes: List<Route>, private val filter: Filter? = null) : RoutingHttpHandler {
    private val routers = routes.map(Route::asRouter)
    private val noMatch: HttpHandler? = null

    override fun withBasePath(basePath: String) = GroupRoutingHttpHandler(UriTemplate.from(basePath),
        routes.map { it.copy(template = it.template.prefixedWith(basePath)) })

    override fun invoke(request: Request): Response = match(request)
        ?.let { handler -> (filter?.then(handler) ?: handler).invoke(request) }
        ?: Response(NOT_FOUND.description("Route not found"))

    override fun match(request: Request): HttpHandler? =
        if (basePath?.matches(request.uri.path) ?: true) routers.fold(noMatch, { memo, router -> memo ?: router.match(request) })
        else null
}

private fun Route.asRouter(): Router = object : Router {
    override fun match(request: Request): HttpHandler? =
        if (template.matches(request.uri.path) && method == request.method) {
            { req: Request -> handler(req.withUriTemplate(template)) }
        } else null
}

private fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = header("x-uri-template", uriTemplate.toString())

internal fun Request.uriTemplate(): UriTemplate = headers.findSingle("x-uri-template")?.let { UriTemplate.from(it) } ?: throw IllegalStateException("x-uri-template header not present in the request")