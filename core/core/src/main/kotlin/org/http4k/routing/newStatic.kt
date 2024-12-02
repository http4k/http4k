package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.experimental.HttpMatchResult
import org.http4k.routing.experimental.NewRouteMatcher
import org.http4k.routing.experimental.Predicate
import org.http4k.routing.experimental.RoutedHttpHandler



/**
 * Serve static content using the passed ResourceLoader. Note that for security, by default ONLY mime-types registered in
 * mime.types (resource file) will be served. All other types are registered as application/octet-stream and are not served.
 */
fun newStatic(
    resourceLoader: ResourceLoader = Classpath(),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutedHttpHandler = RoutedHttpHandler(listOf(NewStaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())))

data class NewStaticRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>): NewRouteMatcher {

    private val handler = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)

    override fun match(request: Request): HttpMatchResult = handler(request).let {
        when {
            it.status != NOT_FOUND -> HttpMatchResult(0) { _: Request -> it }
            else -> HttpMatchResult(2) { _: Request -> Response(NOT_FOUND) }
        }
    }

    override fun withBasePath(prefix: String): NewRouteMatcher = copy(pathSegments = prefix + pathSegments)

    override fun withPredicate(other: Predicate): NewRouteMatcher = this

    override fun toString() = "Static files $pathSegments"
}
