package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.MimeTypes
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.ResourceLoader.Companion.Classpath

/**
 * Serve static content using the passed ResourceLoader. Note that for security, by default ONLY mime-types registered in
 * mime.types (resource file) will be served. All other types are registered as application/octet-stream and are not served.
 */
fun static(
    resourceLoader: ResourceLoader = Classpath(),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutingHttpHandler = StaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())

internal data class StaticRoutingHttpHandler(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val filter: Filter = Filter.NoOp
) : RoutingHttpHandler {

    override val description = RouterDescription("Static files $pathSegments")

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = new.then(filter))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(pathSegments = new + pathSegments)

    private val handlerNoFilter = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)
    private val handlerWithFilter = filter.then(handlerNoFilter)

    override fun match(request: Request): RouterMatch = handlerNoFilter(request).let {
        if (it.status != NOT_FOUND) RouterMatch.MatchingHandler(filter.then { _: Request -> it }, description) else null
    } ?: RouterMatch.Unmatched(description)

    override fun invoke(request: Request): Response = handlerWithFilter(request)
}

internal class ResourceLoadingHandler(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    extraFileExtensionToContentTypes: Map<String, ContentType>
) : HttpHandler {
    private val extMap = MimeTypes(extraFileExtensionToContentTypes)

    override fun invoke(p1: Request): Response = if (p1.uri.path.startsWith(pathSegments)) {
        val path = convertPath(p1.uri.path)
        resourceLoader.load(path)?.let { url ->
            val lookupType = extMap.forFile(path)
            if (p1.method == GET && lookupType != OCTET_STREAM) {
                Response(OK)
                    .with(CONTENT_TYPE of lookupType)
                    .body(Body(url.openStream()))
            } else Response(NOT_FOUND)
        } ?: Response(NOT_FOUND)
    } else Response(NOT_FOUND)

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replaceFirst(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.trimStart('/')
    }
}
