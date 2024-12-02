package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.routing.experimental.HttpMatchResult
import org.http4k.routing.experimental.NewRouteMatcher
import org.http4k.routing.experimental.Predicate
import org.http4k.routing.experimental.RoutedHttpHandler

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun newSinglePageApp(
    resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutedHttpHandler =
    RoutedHttpHandler(
        listOf(
            SinglePageAppRouteMatcher(
                "",
                NewStaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())
            )
        )
    )

internal data class SinglePageAppRouteMatcher(
    private val pathSegments: String,
    private val staticMatcher: NewStaticRouteMatcher
) : NewRouteMatcher {

    override fun match(request: Request): HttpMatchResult {
        val staticMatch = staticMatcher.match(request)
        return when(staticMatch.handler(request).status) {
            Status.NOT_FOUND -> staticMatcher.match(Request(GET, pathSegments))
            else -> staticMatch
        }
    }

//    override fun invoke(request: Request): Response {
//        val matchOnStatic = when (val matchResult = staticHandler.match(request)) {
//            is MatchingHandler -> matchResult(request)
//            else -> null
//        }
//
//        val matchOnIndex = when (val matchResult = staticHandler.match(Request(GET, pathSegments))) {
//            is MatchingHandler -> matchResult
//            else -> null
//        }
//
//        val fallbackHandler = matchOnIndex ?: { Response(NOT_FOUND) }
//        return matchOnStatic ?: fallbackHandler(Request(GET, pathSegments))
//    }

//    override fun match(request: Request) = when (request.method) {
//        OPTIONS -> MethodNotMatched(RouterDescription("template == '$pathSegments'"))
//        else -> MatchingHandler(this, description)
//    }

    override fun withBasePath(new: String) =
        SinglePageAppRouteMatcher(new + pathSegments, staticMatcher.withBasePath(new) as NewStaticRouteMatcher)

    override fun withPredicate(other: Predicate): NewRouteMatcher = this

    override fun toString() = "SPA at $pathSegments"

}
