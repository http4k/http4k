package mcp

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun mcpServer(port: Int = 0) = mcpHttpStreaming(
    ServerMetaData(McpEntity.of("local-fs"), Version.of("1")),
    NoMcpSecurity,
    ReadFile(),
    ListFiles(),
    EditFile(),
    DeleteFile(),
).asServer(JettyLoom(port))

