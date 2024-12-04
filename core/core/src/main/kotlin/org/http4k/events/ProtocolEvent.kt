package org.http4k.events

import org.http4k.core.Uri

/**
 * Represents a traffic event for any protocol that we support.
 */
abstract class ProtocolEvent(
    val uri: Uri,
    val status: ProtocolStatus,
    val latency: Long,
    val xUriTemplate: String,
    val protocol: String
) : Event
