/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import java.time.Duration

internal fun interface McpRpcSender {
    operator fun invoke(
        message: McpJsonRpcMessage,
        timeout: Duration,
        messageId: McpMessageId
    ): McpResult<McpMessageId>
}
