package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutedMessage.Companion.X_URI_TEMPLATE
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

data class SseResponseWithContext(
    private val delegate: SseResponse,
    val context: Map<String, Any>
) : SseResponse by delegate, RoutedMessage {

    constructor(delegate: SseResponse, uriTemplate: UriTemplate) : this(
        if (delegate is SseResponseWithContext) delegate.delegate else delegate,
        if (delegate is SseResponseWithContext) delegate.context + (X_URI_TEMPLATE to uriTemplate)
        else mapOf(X_URI_TEMPLATE to uriTemplate)
    )

    override val xUriTemplate: UriTemplate
        get() {
            return context[X_URI_TEMPLATE] as? UriTemplate
                ?: throw IllegalStateException("Message was not routed, so no uri-template present")
        }

    override fun withConsumer(consumer: SseConsumer) =
        SseResponseWithContext(delegate.withConsumer(consumer), xUriTemplate)

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()
}

class RoutingSseHandler(
    routes: List<RouteMatcher<SseResponse, SseFilter>>
) : RoutingHandler<SseResponse, SseFilter, RoutingSseHandler>(
    routes,
    ::RoutingSseHandler
)

infix fun PathMethod.to(handler: SseHandler) = when (handler) {
    is RoutingSseHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
    else -> RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asRouter())))
}

class TemplatedSseRoute(
    uriTemplate: UriTemplate, handler: SseHandler, router: Router = All, filter: SseFilter = SseFilter.NoOp
) : TemplatedRoute<SseResponse, SseFilter, TemplatedSseRoute>(
    uriTemplate = uriTemplate,
    handler = handler,
    router = router,
    filter = filter,
    responseFor = { SseResponse(it, emptyList(), false, Sse::close) },
    addUriTemplateFilter = { next -> { SseResponseWithContext(next(RequestWithContext(it, uriTemplate)), uriTemplate) } }
) {
    override fun withBasePath(prefix: String) = TemplatedSseRoute(uriTemplate.prefixed(prefix), handler, router, filter)

    override fun withFilter(new: SseFilter) = TemplatedSseRoute(uriTemplate, handler, router, new.then(filter))

    override fun withRouter(other: Router) = TemplatedSseRoute(uriTemplate, handler, router.and(other), filter)

}
