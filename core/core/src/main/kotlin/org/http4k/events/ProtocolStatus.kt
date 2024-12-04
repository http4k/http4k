package org.http4k.events

/**
 * Represents a status for any protocol that we support.
 */
interface ProtocolStatus {
    val code: Int
    val description: String
}
