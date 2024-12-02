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
fun newStatic(
    resourceLoader: ResourceLoader = Classpath(),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutingHttpHandler = NewStaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())

data class NewStaticRoutingHttpHandler(
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

    override fun toString() = description.friendlyToString()
}
