package org.http4k.lens

import org.http4k.mcp.protocol.SessionId

val Header.MCP_SESSION_ID get() = value(SessionId).optional("Mcp-Session-Id")

