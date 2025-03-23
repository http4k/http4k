package org.http4k.mcp.server.sessions

import org.http4k.mcp.protocol.SessionId

/**
 * Represents a session for a connection.
 */
sealed interface Session {

    sealed interface Valid : Session {
        val sessionId: SessionId

        data class Existing(override val sessionId: SessionId) : Valid

        data class New(override val sessionId: SessionId) : Valid
    }

    data object Invalid : Session
}
