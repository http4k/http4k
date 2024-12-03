package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.routing.sse.RoutingSseHandler
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse

fun sse(sse: SseConsumer): SseHandler = { SseResponse(sse) }

fun sse(vararg methods: Pair<Method, SseHandler>): SseHandler = {
    methods.toMap()[it.method]?.invoke(it) ?: SseResponse { it.close() }
}

fun sse(vararg list: RoutingSseHandler): RoutingSseHandler = sse(list.toList())

fun sse(routers: List<RoutingSseHandler>) =
    RoutingSseHandler(routers.flatMap { it.routes })

class RoutedSseResponse(
    val delegate: SseResponse,
    override val xUriTemplate: UriTemplate,
) : SseResponse by delegate, RoutedMessage {
    override fun withConsumer(consumer: SseConsumer) =
        RoutedSseResponse(delegate.withConsumer(consumer), xUriTemplate)

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()
}
