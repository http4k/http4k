package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.sse.NoOp
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

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
    override fun withConsumer(consumer: SseConsumer) = RoutedSseResponse(delegate.withConsumer(consumer), xUriTemplate)

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()
}

class RoutingSseHandler(
    routes: List<RouteMatcher<SseResponse, SseFilter>>
) : RoutingHandler<SseResponse, SseFilter, RoutingSseHandler>(
    routes,
    ::RoutingSseHandler
)

data class SsePathMethod(val path: String, val method: Method) {
    infix fun to(handler: SseHandler) = when (handler) {
        is RoutingSseHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
        else -> RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asRouter())))
    }
}

class TemplatedSseRoute(
    uriTemplate: UriTemplate, handler: SseHandler, router: Router = All, filter: SseFilter = SseFilter.NoOp
) : TemplatedRoute<SseResponse, SseFilter, TemplatedSseRoute>(
    uriTemplate = uriTemplate,
    handler = handler,
    router = router,
    filter = filter,
    responseFor = { SseResponse(it, emptyList(), false, Sse::close) },
    addUriTemplateFilter = { next -> { RoutedSseResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate) } }
) {
    override fun withBasePath(prefix: String) = TemplatedSseRoute(uriTemplate.prefixed(prefix), handler, router, filter)

    override fun withFilter(new: SseFilter) = TemplatedSseRoute(uriTemplate, handler, router, new.then(filter))

    override fun withRouter(other: Router) = TemplatedSseRoute(uriTemplate, handler, router.and(other), filter)

}
