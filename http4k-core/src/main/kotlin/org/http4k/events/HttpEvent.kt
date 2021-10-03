package org.http4k.events

import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri

sealed class HttpEvent(
    val uri: Uri,
    val method: Method,
    val status: Status,
    val latency: Long,
) : Event {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HttpEvent

        if (uri != other.uri) return false
        if (method != other.method) return false
        if (status != other.status) return false
        if (latency != other.latency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + latency.hashCode()
        return result
    }

    class Incoming(
        uri: Uri,
        method: Method,
        status: Status,
        latency: Long
    ) : HttpEvent(uri, method, status, latency) {
        constructor(tx: HttpTransaction) : this(
            tx.request.uri,
            tx.request.method,
            tx.response.status,
            tx.duration.toMillis()
        )
    }

    class Outgoing(
        uri: Uri,
        method: Method,
        status: Status,
        latency: Long
    ) : HttpEvent(uri, method, status, latency) {
        constructor(tx: HttpTransaction) : this(
            tx.request.uri,
            tx.request.method,
            tx.response.status,
            tx.duration.toMillis()
        )
    }
}
