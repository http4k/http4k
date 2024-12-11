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
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched


/**
 * Serve static content using the passed ResourceLoader. Note that for security, by default ONLY mime-types registered in
 * mime.types (resource file) will be served. All other types are registered as application/octet-stream and are not served.
 */
fun static(
    resourceLoader: ResourceLoader = Classpath(),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
) = RoutingHttpHandler(
    listOf(
        StaticRouteMatcher(
            "",
            resourceLoader,
            extraFileExtensionToContentTypes.asList().toMap()
        )
    )
)

data class StaticRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val router: Router = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher<Response, Filter>{

    private val handler = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)

    override fun match(request: Request) = when (val result = router(request)) {
        is Matched -> handler(request).let {
            when {
                it.status != NOT_FOUND -> RoutingMatch(0, result.description, filter.then { _: Request -> it })
                else -> RoutingMatch(2, result.description, filter.then { _: Request -> Response(NOT_FOUND) })
            }
        }

        is NotMatched -> RoutingMatch(2, result.description, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher<Response, Filter> = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = copy(filter = new.then(filter))
    override fun withRouter(other: Router): RouteMatcher<Response, Filter> = copy(router = router.and(other))

    override fun toString() = "static files at $pathSegments"
}

internal class ResourceLoadingHandler(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    extraFileExtensionToContentTypes: Map<String, ContentType>
) : HttpHandler {
    private val extMap = MimeTypes(extraFileExtensionToContentTypes)

    override fun invoke(p1: Request): Response = if (isStartingWithPathSegment(p1) && p1.method == GET) {
        load(convertPath(p1.uri.path))
    } else Response(NOT_FOUND)

    private fun load(path: String): Response =
        loadPath(path)
            ?: loadPath(pathWithIndex(path))
            ?: Response(NOT_FOUND)

    private fun loadPath(path: String): Response? =
        resourceLoader.load(path)?.let { url ->
            val lookupType = extMap.forFile(path)
            if (lookupType != OCTET_STREAM) {
                Response(OK)
                    .with(CONTENT_TYPE of lookupType)
                    .body(Body(url.openStream()))
            } else null
        }

    private fun pathWithIndex(path: String): String {
        val newPath = if (path.endsWith("/")) "${path}index.html" else "$path/index.html"
        return newPath.trimStart('/')
    }

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replaceFirst(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.trimStart('/')
    }

    private fun isStartingWithPathSegment(p1: Request) =
        (p1.uri.path.startsWith(pathSegments) || p1.uri.path.startsWith("/$pathSegments"))
}
