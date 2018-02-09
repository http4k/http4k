package org.http4k.core

import org.http4k.lens.Header
import java.time.Duration

data class HttpTransaction(val request: Request, val response: Response, val duration: Duration, val labels: Map<String, String> =
Header.X_URI_TEMPLATE(request)?.let { it -> mapOf(ROUTING_GROUP to it) } ?: emptyMap()) {
    fun label(name: String, value: String) = copy(labels = labels + (name to value))
    fun label(name: String) = labels[name]

    val routingGroup by lazy { labels[ROUTING_GROUP] ?: "UNMAPPED" }

    companion object {
        const val ROUTING_GROUP = "routingGroup"
    }
}