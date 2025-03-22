package org.http4k.mcp.server.protocol

import org.http4k.mcp.protocol.SessionId

sealed interface Session {

    sealed interface Valid : Session {
        val headerValue: String
        val sessionId: SessionId

        data class Existing(override val sessionId: SessionId) : Valid {
            override val headerValue = "resumed"
        }

        data class New(override val sessionId: SessionId) : Valid {
            override val headerValue = "created"
        }
    }

    data object Invalid : Session
}
