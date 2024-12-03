package org.http4k.routing.sse

import org.http4k.sse.SseHandler

internal data class SseMatchResult(val priority: Int, val handler: SseHandler)
