/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.server.protocol.SessionState.Valid

/**
 * Represents a session for a connection.
 */
sealed interface SessionState {
    sealed interface Valid : SessionState {
        val session: Session
    }

    data object Invalid : SessionState
}

data class ExistingSession(override val session: Session) : Valid

data class NewSession(override val session: Session) : Valid

