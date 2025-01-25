package org.http4k.core

import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import java.time.Duration
import java.time.Instant

data class SseTransaction(
    override val request: Request,
    override val response: SseResponse,
    override val duration: Duration,
    override val labels: Map<String, String> = defaultLabels(request, response),
    override val start: Instant
) : ProtocolTransaction<SseResponse> {
    fun label(name: String, value: String) = copy(labels = labels + (name to value))
}

fun SseFilter.then(poly: PolyHandler): PolyHandler = poly.copy(sse = poly.sse?.let { then(it) })
