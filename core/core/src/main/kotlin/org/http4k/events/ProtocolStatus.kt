package org.http4k.events

/**
 * Represents a status for any protocol that we support.
 */
data class ProtocolStatus(val code: Int, val description: String, val clientGenerated: Boolean = false) {
    override fun hashCode(): Int = code.hashCode() + clientGenerated.hashCode()

    override fun toString(): String = "$code $description"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProtocolStatus

        if (code != other.code) return false
        if (clientGenerated != other.clientGenerated) return false

        return true
    }
}
