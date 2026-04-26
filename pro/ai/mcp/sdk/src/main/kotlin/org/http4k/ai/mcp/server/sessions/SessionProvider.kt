/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.sessions

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.protocol.ExistingSession
import org.http4k.ai.mcp.server.protocol.McpSessionState
import org.http4k.ai.mcp.server.protocol.NewSession
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.core.Request
import java.util.UUID
import kotlin.random.Random

/**
 * Provides a session identifier for a given connection request. This can be used to allocate a particular session
 * which can be used to track the connection. Handles both new and existing sessions, as well as validating
 * if a particular request is authorised to connect to that session.
 */
fun interface SessionProvider {
    fun validate(connectRequest: Request, sessionId: SessionId?): McpSessionState

    companion object {
        /**
         * Provides a totally random session identifier.
         */
        fun Random(random: Random) =
            SessionProvider { connectRequest, sessionId ->
                    when (sessionId) {
                        null -> NewSession(Session(SessionId.of(UUID(random.nextLong(), random.nextLong()).toString())))
                        else -> ExistingSession(Session(sessionId))
                    }
            }
    }
}
