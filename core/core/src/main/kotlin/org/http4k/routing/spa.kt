package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun singlePageApp(
    resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutingHttpHandler =
    RoutingHttpHandler(
        listOf(
            SinglePageAppRouteMatcher(
                "",
                StaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())
            )
        )
    )

internal data class SinglePageAppRouteMatcher(
    private val pathSegments: String,
    private val staticMatcher: StaticRouteMatcher
) : RouteMatcher {

    override fun match(request: Request): HttpMatchResult {
        val staticMatch = staticMatcher.match(request)
        return when (staticMatch.handler(request).status) {
            Status.NOT_FOUND -> staticMatcher.match(Request(GET, pathSegments))
            else -> staticMatch
        }
    }

    override fun withBasePath(new: String) =
        SinglePageAppRouteMatcher(new + pathSegments, staticMatcher.withBasePath(new) as StaticRouteMatcher)

    override fun withPredicate(other: Predicate): RouteMatcher = this

    override fun toString() = "SPA at $pathSegments"

}
