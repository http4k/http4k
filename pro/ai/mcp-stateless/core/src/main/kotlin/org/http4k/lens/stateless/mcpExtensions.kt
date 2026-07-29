/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens.stateless

import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.stateless.protocol.decodeMcpHeaderValue
import org.http4k.lens.Header
import org.http4k.lens.string
import org.http4k.lens.value

val Header.MCP_PROTOCOL_VERSION get() = value(ProtocolVersion).defaulted("Mcp-Protocol-Version", LATEST_VERSION)

val Header.MCP_METHOD get() = value(McpRpcMethod).optional("Mcp-Method")

val Header.MCP_NAME get() = string().map({ it.trim().let { v -> decodeMcpHeaderValue(v) ?: v } }, { it })
    .optional("Mcp-Name")
