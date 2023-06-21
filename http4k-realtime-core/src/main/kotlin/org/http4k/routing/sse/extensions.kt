package org.http4k.routing.sse

import org.http4k.core.UriTemplate
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.TemplateRoutingSseHandler
import org.http4k.sse.SseHandler

infix fun String.bind(handler: SseHandler): RoutingSseHandler =
    TemplateRoutingSseHandler(UriTemplate.from(this), handler)

infix fun String.bind(sseHandler: RoutingSseHandler): RoutingSseHandler = sseHandler.withBasePath(this)
