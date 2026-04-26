/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import se.ansman.kotshi.JsonSerializable

sealed interface ServerMessage {
    interface Request : ServerMessage, McpRequest
    interface Response : ServerMessage, McpResponse {
        @JsonSerializable
        data object Empty : Response
    }

    interface Notification : ServerMessage, McpNotification
}

