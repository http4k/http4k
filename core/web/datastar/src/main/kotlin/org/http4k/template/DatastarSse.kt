package org.http4k.template

import org.http4k.core.Request
import org.http4k.sse.SseHandler

/**
 * Custom SseHandler for Datastar when used with TemplateRenderers
 */
fun DatastarSse(renderer: TemplateRenderer, fn: (Request) -> DatastarSseResponse): SseHandler = { fn(it)(renderer) }
