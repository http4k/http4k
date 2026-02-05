package org.http4k.ai.mcp.server.sessions

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.SessionState
import org.http4k.core.Request
import java.util.UUID
import kotlin.random.Random

/**
 * Provides a session identifier for a given connection request. This can be used to allocate a particular session
 * which can be used to track the connection. Handles both new and existing sessions, as well as validating
 * if a particular request is authorised to connect to that session.
 */
fun interface SessionProvider {
    fun validate(connectRequest: Request, sessionId: SessionId?): SessionState

    companion object {
        /**
         * Provides a totally random session identifier.
         */
        fun Random(random: Random) =
            SessionProvider { connectRequest, sessionId ->
                Session(
                    when (sessionId) {
                        null -> SessionId.of(UUID(random.nextLong(), random.nextLong()).toString())
                        else -> sessionId
                    }
                )
            }
    }
}
