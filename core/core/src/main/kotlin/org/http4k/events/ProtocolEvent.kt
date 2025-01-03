package org.http4k.events

import org.http4k.core.Method
import org.http4k.core.Uri

/**
 * Represents a traffic event for any protocol that we support.
 */
sealed class ProtocolEvent(
    val uri: Uri,
    val method: Method,
    val status: ProtocolStatus,
    val latency: Long,
    val xUriTemplate: String,
    val protocol: String
) : Event {
    class Incoming(
        uri: Uri,
        method: Method,
        status: ProtocolStatus,
        latency: Long,
        xUriTemplate: String,
        protocol: String
    ) : ProtocolEvent(uri, method, status, latency, xUriTemplate, protocol) {
        override fun toString() =
            "Incoming(uri=$uri, method=$method, status=$status, latency=$latency, xUriTemplate=$xUriTemplate, protocol=$protocol)"

        companion object
    }

    class Outgoing(
        uri: Uri,
        method: Method,
        status: ProtocolStatus,
        latency: Long,
        xUriTemplate: String,
        protocol: String
    ) : ProtocolEvent(uri, method, status, latency, xUriTemplate, protocol) {
        override fun toString() = "Outgoing(uri=$uri, method=$method, status=$status, latency=$latency, xUriTemplate=$xUriTemplate, protocol=$protocol)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProtocolEvent

        if (uri != other.uri) return false
        if (method != other.method) return false
        if (status != other.status) return false
        if (latency != other.latency) return false
        if (xUriTemplate != other.xUriTemplate) return false
        if (protocol != other.protocol) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + latency.hashCode()
        result = 31 * result + xUriTemplate.hashCode()
        result = 31 * result + protocol.hashCode()
        return result
    }
}
