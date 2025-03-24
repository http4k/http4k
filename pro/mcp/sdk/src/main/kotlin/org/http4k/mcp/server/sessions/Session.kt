package org.http4k.mcp.server.sessions

import org.http4k.mcp.protocol.SessionId

/**
 * Represents a session for a connection.
 */
sealed interface Session {
    data class Valid(val sessionId: SessionId) : Session
    data object Invalid : Session
}
