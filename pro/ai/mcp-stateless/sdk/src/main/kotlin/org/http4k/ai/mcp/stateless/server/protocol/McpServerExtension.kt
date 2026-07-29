/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

import org.http4k.ai.mcp.stateless.protocol.McpExtension
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod

interface McpServerExtension : McpExtension, McpHandler {
    val methods: Set<McpRpcMethod>
}
