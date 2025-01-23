package org.http4k.routing.sse

import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.SseRouter
import org.http4k.routing.sse
import org.http4k.routing.sse.SseRouterMatch.MatchingHandler
import org.http4k.routing.sse.SseRouterMatch.Unmatched
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

sealed class SseRouterMatch(private val priority: Int) : Comparable<SseRouterMatch> {

    data class MatchingHandler(private val handler: SseHandler) : SseRouterMatch(0), SseHandler by handler

    data object Unmatched : SseRouterMatch(1)

    override fun compareTo(other: SseRouterMatch): Int = priority.compareTo(other.priority)
}

internal class RouterSseHandler(private val list: List<SseRouter>) : RoutingSseHandler {
    override fun match(request: Request) = list.minOfOrNull { it.match(request) } ?: Unmatched

    override operator fun invoke(request: Request): SseResponse = when (val match = match(request)) {
        is MatchingHandler -> match(request)
        is Unmatched -> SseResponse(NOT_FOUND, handled = false) { it.close() }
    }

    override fun withBasePath(new: String): RoutingSseHandler =
        sse(*list.map { it.withBasePath(new) }.toTypedArray())

    override fun withFilter(new: SseFilter) = RouterSseHandler(list.map { it.withFilter(new) })
}

internal class TemplateRoutingSseHandler(
    private val template: UriTemplate,
    private val handler: SseHandler
) : RoutingSseHandler {
    override fun match(request: Request): SseRouterMatch = when {
        template.matches(request.uri.path) -> MatchingHandler { req -> handler(RoutedRequest(req, template)) }

        else -> Unmatched
    }

    override operator fun invoke(request: Request): SseResponse = when (val matched = match(request)) {
        is MatchingHandler -> matched(RoutedRequest(request, template))
        is Unmatched -> SseResponse(NOT_FOUND, handled = false) { it.close() }
    }

    override fun withBasePath(new: String) = TemplateRoutingSseHandler(UriTemplate.from("$new/$template"), handler)

    override fun withFilter(new: SseFilter) = TemplateRoutingSseHandler(template, new.then(handler))
}
