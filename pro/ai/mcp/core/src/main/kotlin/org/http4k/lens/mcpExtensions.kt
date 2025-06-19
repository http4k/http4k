package org.http4k.lens

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId

val Header.MCP_SESSION_ID get() = value(SessionId).optional("Mcp-Session-Id")

val Header.MCP_PROTOCOL_VERSION get() = value(ProtocolVersion).defaulted("MCP-Protocol-Version", LATEST_VERSION)

