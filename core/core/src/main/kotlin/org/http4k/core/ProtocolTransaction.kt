package org.http4k.core

import org.http4k.core.ProtocolTransaction.Companion.ROUTING_GROUP_LABEL
import org.http4k.routing.uriTemplate
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

fun defaultLabels(request: Request, responseUriTemplate: UriTemplate?) = when {
    responseUriTemplate != null ->
        mapOf(ROUTING_GROUP_LABEL to responseUriTemplate.toString())
    request.uriTemplate() != null ->
        request.uriTemplate()?.let { mapOf(ROUTING_GROUP_LABEL to it.toString()) } ?: emptyMap()
    else -> emptyMap()
}
