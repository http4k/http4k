/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

sealed interface ClientMessage {
    interface Request : ClientMessage, McpRequest
    interface Response : ClientMessage, McpResponse
    interface Notification : ClientMessage, McpNotification
}

