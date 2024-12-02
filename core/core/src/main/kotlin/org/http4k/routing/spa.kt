package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun singlePageApp(
    resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutingHttpHandler =
    RoutingHttpHandler(
        listOf(SinglePageAppRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap()))
    )

internal data class SinglePageAppRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: ResourceLoader,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val predicate: Predicate = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher {

    private val handler = ResourceLoadingHandler(pathSegments, resourceLoader, extraFileExtensionToContentTypes)

    override fun match(request: Request): HttpMatchResult = when (predicate(request)) {
        is Matched -> {
            handler(request).let {
                when {
                    it.status != NOT_FOUND -> HttpMatchResult(0, filter.then { _: Request -> it })
                    else -> HttpMatchResult(2, filter.then { _: Request -> handler(Request(GET, pathSegments)) })
                }
            }
        }

        is NotMatched -> HttpMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))
    override fun withPredicate(other: Predicate): RouteMatcher = copy(predicate = predicate.and(other))

    override fun toString() = "SPA at $pathSegments"

}
