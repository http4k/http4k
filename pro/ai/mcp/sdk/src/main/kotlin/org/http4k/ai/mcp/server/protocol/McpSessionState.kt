/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.server.protocol.McpSessionState.Valid

/**
 * Represents a session for a connection.
 */
sealed interface McpSessionState {
    sealed interface Valid : McpSessionState {
        val session: Session
    }

    data object Invalid : McpSessionState
}

data class ExistingSession(override val session: Session) : Valid

data class NewSession(override val session: Session) : Valid

