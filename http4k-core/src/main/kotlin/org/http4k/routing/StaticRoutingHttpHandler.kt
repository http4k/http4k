package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then

/**
 * Serve static content using the passed ResourceLoader. Note that for security, by default ONLY mime-types registered in
 * mime.types (resource file) will be served. All other types are registered as application/octet-stream and are not served.
 */
fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraFileExtensionToContentTypes: Pair<String, ContentType>): RoutingHttpHandler = StaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())

internal data class StaticRoutingHttpHandler(private val pathSegments: String,
                                             private val resourceLoader: ResourceLoader,
                                             private val extraFileExtensionToContentTypes: Map<String, ContentType>,
                                             private val filter: Filter = Filter.NoOp
) : RoutingHttpHandler {

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = new.then(filter))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(pathSegments = new + pathSegments)

    private val handlerNoFilter = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)
    private val handlerWithFilter = filter.then(handlerNoFilter)

    override fun match(request: Request): RouterMatch = handlerNoFilter(request).let {
        if (it.status != Status.NOT_FOUND) RouterMatch.MatchingHandler(filter.then { _: Request -> it }) else null
    } ?: RouterMatch.Unmatched

    override fun invoke(request: Request): Response = handlerWithFilter(request)
}
