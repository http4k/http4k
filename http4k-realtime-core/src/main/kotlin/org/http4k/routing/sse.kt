package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.sse.Sse
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler

interface RoutingSseHandler : SseHandler {
    fun withBasePath(new: String): RoutingSseHandler
}

infix fun String.bind(consumer: SseConsumer): RoutingSseHandler =
    TemplateRoutingSseHandler(UriTemplate.from(this), consumer)

infix fun String.bind(sseHandler: RoutingSseHandler): RoutingSseHandler = sseHandler.withBasePath(this)

fun sse(sse: SseConsumer): SseHandler = { sse }

fun sse(vararg list: RoutingSseHandler): RoutingSseHandler = object : RoutingSseHandler {
    override operator fun invoke(request: Request): SseConsumer? = list.firstOrNull { it(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingSseHandler = sse(*list.map { it.withBasePath(new) }.toTypedArray())
}

internal data class TemplateRoutingSseHandler(private val template: UriTemplate,
                                             private val consumer: SseConsumer) : RoutingSseHandler {
    override operator fun invoke(request: Request): SseConsumer? = if (template.matches(request.uri.path)) { sse ->
        consumer(object : Sse by sse {
            override val connectRequest: Request = RoutedRequest(sse.connectRequest, template)
        })
    } else null

    override fun withBasePath(new: String): TemplateRoutingSseHandler = copy(template = UriTemplate.from("$new/$template"))
}
