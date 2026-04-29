/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens

import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId

val Header.MCP_SESSION_ID get() = value(SessionId).optional("Mcp-Session-Id")

val Header.MCP_PROTOCOL_VERSION get() = value(ProtocolVersion).defaulted("Mcp-Protocol-Version", LATEST_VERSION)

val Header.MCP_METHOD get() = value(McpRpcMethod).optional("Mcp-Method")

val Header.MCP_NAME get() = string().optional("Mcp-Name")

