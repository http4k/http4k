package org.http4k.core

import org.http4k.routing.RequestWithRoute
import org.http4k.routing.ResponseWithRoute
import org.http4k.routing.RoutedResponse
import java.time.Duration

data class HttpTransaction(
    val request: Request,
    val response: Response,
    val duration: Duration,
    val labels: Map<String, String> = when {
        response is ResponseWithRoute -> mapOf(ROUTING_GROUP_LABEL to response.xUriTemplate.toString())
        request is RequestWithRoute -> mapOf(ROUTING_GROUP_LABEL to request.xUriTemplate.toString())
        else -> emptyMap()
    }
) {
    fun label(name: String, value: String) = copy(labels = labels + (name to value))
    fun label(name: String) = labels[name]

    val routingGroup = labels[ROUTING_GROUP_LABEL] ?: "UNMAPPED"

    companion object {
        internal const val ROUTING_GROUP_LABEL = "routingGroup"
    }
}
