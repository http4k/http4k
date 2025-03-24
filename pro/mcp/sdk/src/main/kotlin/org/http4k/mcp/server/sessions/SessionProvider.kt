package org.http4k.mcp.server.sessions

import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.AuthedSession
import org.http4k.mcp.server.protocol.SessionState
import java.util.UUID
import kotlin.random.Random

/**
 * Provides a session identifier for a given connection request. This can be used to allocate a particular session
 * which can be used to track the connection. Handles both new and existing sessions, as well as validating
 * if a particular request is authorised to connect to that session.
 */
interface SessionProvider {
    fun validate(connectRequest: Request, sessionId: SessionId?): SessionState

    companion object {
        /**
         * Provides a totally random session identifier.
         */
        fun Random(random: Random) =
            object : SessionProvider {
                override fun validate(connectRequest: Request, sessionId: SessionId?) =
                    AuthedSession(
                        when (sessionId) {
                            null -> SessionId.of(UUID(random.nextLong(), random.nextLong()).toString())
                            else -> sessionId
                        }
                    )
            }
    }
}
