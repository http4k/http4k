package org.http4k.events

import org.http4k.core.Method
import org.http4k.core.SseTransaction
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.routing.RoutedMessage

object SseEvent {
    fun Incoming(
        uri: Uri,
        method: Method,
        status: Status,
        latency: Long,
        xUriTemplate: String,
    ) = ProtocolEvent.Incoming(uri, method, status, latency, xUriTemplate, "sse")

    fun Incoming(tx: SseTransaction) = Incoming(
        tx.request.uri,
        tx.request.method,
        tx.response.status,
        tx.duration.toMillis(),
        if (tx.request is RoutedMessage && tx.request.xUriTemplate != null)
            tx.request.xUriTemplate.toString() else tx.request.uri.path.trimStart('/')
    )

    fun Outgoing(
        uri: Uri,
        method: Method,
        status: Status,
        latency: Long,
        xUriTemplate: String,
    ) = ProtocolEvent.Outgoing(uri, method, status, latency, xUriTemplate, "http")

    fun Outgoing(tx: SseTransaction) = Outgoing(
        tx.request.uri,
        tx.request.method,
        tx.response.status,
        tx.duration.toMillis(),
        if (tx.response is RoutedMessage && tx.response.xUriTemplate != null)
            tx.response.xUriTemplate.toString() else tx.request.uri.path.trimStart('/')
    )
}
