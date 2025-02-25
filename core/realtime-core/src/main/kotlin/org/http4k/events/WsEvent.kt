package org.http4k.events

import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.WsTransaction
import org.http4k.routing.RequestWithContext
import org.http4k.routing.RoutedMessage
import org.http4k.routing.uriTemplate
import org.http4k.websocket.WsStatus

object WsEvent {
    fun Incoming(
        uri: Uri,
        method: Method,
        status: WsStatus,
        latency: Long,
        xUriTemplate: String,
    ) = ProtocolEvent.Incoming(uri, method, status, latency, xUriTemplate, "ws")

    fun Incoming(tx: WsTransaction) = Incoming(
        tx.request.uri,
        tx.request.method,
        tx.status,
        tx.duration.toMillis(),
        if (tx.request.uriTemplate() != null)
            tx.request.uriTemplate().toString() else tx.request.uri.path.trimStart('/')
    )

    fun Outgoing(
        uri: Uri,
        method: Method,
        status: WsStatus,
        latency: Long,
        xUriTemplate: String,
    ) = ProtocolEvent.Outgoing(uri, method, status, latency, xUriTemplate, "http")

    fun Outgoing(tx: WsTransaction) = Outgoing(
        tx.request.uri,
        tx.request.method,
        tx.status,
        tx.duration.toMillis(),
        if (tx.response.uriTemplate() != null)
            tx.response.uriTemplate().toString() else tx.request.uri.path.trimStart('/')
    )
}
