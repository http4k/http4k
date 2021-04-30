package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.SseRouterMatch.MatchingHandler
import org.http4k.routing.SseRouterMatch.Unmatched
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer

sealed class SseRouterMatch(private val priority: Int) : Comparable<SseRouterMatch> {

    data class MatchingHandler(private val wsHandler: SseConsumer) : SseRouterMatch(0), SseConsumer by wsHandler

    object Unmatched : SseRouterMatch(1)

    override fun compareTo(other: SseRouterMatch): Int = priority.compareTo(other.priority)
}

internal class RouterSseHandler(private val list: List<SseRouter>) : RoutingSseHandler {
    override fun match(request: Request) = list.map { next -> next.match(request) }.minOrNull() ?: Unmatched

    override operator fun invoke(request: Request): SseConsumer = when (val match = match(request)) {
        is MatchingHandler -> match
        is Unmatched -> { it: Sse -> it.close() }
    }

    override fun withBasePath(new: String): RoutingSseHandler =
        sse(*list.map { it.withBasePath(new) }.toTypedArray())
}

internal class TemplateRoutingSseHandler(
    private val template: UriTemplate,
    private val consumer: SseConsumer
) : RoutingSseHandler {
    override fun match(request: Request): SseRouterMatch = when {
        template.matches(request.uri.path) -> MatchingHandler { sse ->
            consumer(object : Sse by sse {
                override val connectRequest: Request = RoutedRequest(sse.connectRequest, template)
            })
        }
        else -> Unmatched
    }

    override operator fun invoke(request: Request): SseConsumer = when (val match = match(request)) {
        is MatchingHandler -> match
        is Unmatched -> { it: Sse -> it.close() }
    }

    override fun withBasePath(new: String) = TemplateRoutingSseHandler(UriTemplate.from("$new/$template"), consumer)
}
