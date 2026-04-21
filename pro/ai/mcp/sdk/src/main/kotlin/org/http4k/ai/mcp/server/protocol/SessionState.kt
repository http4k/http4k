/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.SessionId

/**
 * Represents a session for a connection.
 */
sealed interface SessionState

sealed interface ValidSessionState : SessionState {
    val session: Session
}

data class ExistingSession(override val session: Session) : ValidSessionState

data class NewSession(override val session: Session) : ValidSessionState

data object InvalidSessionState

data class Session(val id: SessionId)

