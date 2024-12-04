package org.http4k.events

import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.WsTransaction
import org.http4k.routing.RoutedMessage
import org.http4k.routing.RoutedRequest
import org.http4k.websocket.WsStatus

sealed class WsEvent(
     uri: Uri,
    val method: Method,
     status: WsStatus,
     latency: Long,
     xUriTemplate: String,
) : ProtocolEvent(uri, ProtocolStatus(status.code, status.description, false), latency, xUriTemplate, "ws") {

    class Incoming(
        uri: Uri,
        method: Method,
        status: WsStatus,
        latency: Long,
        xUriTemplate: String,
    ) : WsEvent(uri, method, status, latency, xUriTemplate) {
        constructor(tx: WsTransaction) : this(
            tx.request.uri,
            tx.request.method,
            tx.status,
            tx.duration.toMillis(),
            if (tx.request is RoutedRequest) tx.request.xUriTemplate.toString() else tx.request.uri.path.trimStart('/')
        )

        override fun toString() =
            "Incoming(uri=$uri, method=$method, status=$status, latency=$latency, xUriTemplate=$xUriTemplate, protocol=$protocol)"

        companion object
    }

    class Outgoing(
        uri: Uri,
        method: Method,
        status: WsStatus,
        latency: Long,
        xUriTemplate: String,
    ) : WsEvent(uri, method, status, latency, xUriTemplate) {
        constructor(tx: WsTransaction) : this(
            tx.request.uri,
            tx.request.method,
            tx.status,
            tx.duration.toMillis(),
            if (tx.response is RoutedMessage) tx.response.xUriTemplate.toString() else tx.request.uri.path.trimStart('/')
        )

        override fun toString() =
            "Outgoing(uri=$uri, method=$method, status=$status, latency=$latency, xUriTemplate=$xUriTemplate,protocol=$protocol)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WsEvent

        if (uri != other.uri) return false
        if (method != other.method) return false
        if (status != other.status) return false
        if (latency != other.latency) return false
        if (xUriTemplate != other.xUriTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + latency.hashCode()
        result = 31 * result + xUriTemplate.hashCode()
        return result
    }
}
