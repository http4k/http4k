package org.http4k.mcp.server.protocol

import org.http4k.mcp.protocol.SessionId

/**
 * Represents a session for a connection.
 */
sealed interface SessionState

data class AuthedSession(val id: SessionId) : SessionState
data object InvalidSession : SessionState
