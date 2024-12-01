package org.http4k.core

import org.http4k.core.ProtocolTransaction.Companion.ROUTING_GROUP_LABEL
import org.http4k.routing.RoutedMessage
import java.time.Duration
import java.time.Instant

interface ProtocolTransaction<ProtocolResponse> {
    val request: Request
    val response: ProtocolResponse
    val duration: Duration
    val start: Instant
    val labels: Map<String, String>

    fun label(name: String) = labels[name]

    val routingGroup get() = labels[ROUTING_GROUP_LABEL] ?: "UNMAPPED"

    companion object {
        internal const val ROUTING_GROUP_LABEL = "routingGroup"
    }
}

fun <ProtocolResponse> defaultLabels(request: Request, response: ProtocolResponse) = when {
    response is RoutedMessage -> mapOf(ROUTING_GROUP_LABEL to response.xUriTemplate.toString())
    request is RoutedMessage -> mapOf(ROUTING_GROUP_LABEL to request.xUriTemplate.toString())
    else -> emptyMap()
}
