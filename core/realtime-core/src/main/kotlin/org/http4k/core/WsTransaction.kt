package org.http4k.core

import org.http4k.websocket.WsResponse
import java.time.Duration
import java.time.Instant

data class WsTransaction(
    override val request: Request,
    override val response: WsResponse,
    override val duration: Duration,
    override val labels: Map<String, String> = defaultLabels(request, response),
    override val start: Instant
) : ProtocolTransaction<WsResponse> {
    fun label(name: String, value: String) = copy(labels = labels + (name to value))
}
