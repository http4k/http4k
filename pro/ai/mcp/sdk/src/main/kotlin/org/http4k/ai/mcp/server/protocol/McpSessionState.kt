/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

/**
 * Represents a session for a connection.
 */
sealed interface McpSessionState {
    sealed interface Valid : McpSessionState {
        val session: Session

        data class Existing(override val session: Session) : Valid

        data class New(override val session: Session) : Valid

    }

    data object Invalid : McpSessionState
}


