package org.http4k.core

import java.time.Duration
import java.time.Instant

data class HttpTransaction(
    override val request: Request,
    override val response: Response,
    override val duration: Duration,
    override val labels: Map<String, String> = defaultLabels(request, response),
    override val start: Instant
) : ProtocolTransaction<Response> {
    fun label(name: String, value: String) = copy(labels = labels + (name to value))
}
