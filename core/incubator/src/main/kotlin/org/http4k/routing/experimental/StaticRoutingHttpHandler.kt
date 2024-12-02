package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri.Companion.of
import org.http4k.core.then
import org.http4k.routing.All
import org.http4k.routing.HttpMatchResult
import org.http4k.routing.Predicate
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched
import org.http4k.routing.RouteMatcher
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.and

fun static(
    resourceLoader: RouteMatcher,
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
) = RoutingHttpHandler(
    listOf(
        NewStaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())
    )
)

data class NewStaticRouteMatcher(
    private val pathSegments: String,
    private val resourceLoader: RouteMatcher,
    private val extraFileExtensionToContentTypes: Map<String, ContentType>,
    private val predicate: Predicate = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher {
    override fun match(request: Request): HttpMatchResult = when (predicate(request)) {
        is Matched -> resourceLoader.match(request.uri(of(convertPath(request.uri.path))))
        is NotMatched -> HttpMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String): RouteMatcher = copy(pathSegments = prefix + pathSegments)
    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))
    override fun withPredicate(other: Predicate): RouteMatcher = copy(predicate = predicate.and(other))

    private fun convertPath(path: String) =
        if (pathSegments == "/" || pathSegments == "") path else path.replace(pathSegments, "")

    override fun toString() = "Static files $pathSegments"
}
