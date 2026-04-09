/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.core.Request

/**
 * Handles protocol traffic for client server handshaking and session initialization.
 */
interface Initializer {
    operator fun invoke(req: McpInitialize.Request, http: Request): McpInitialize.Response
}

