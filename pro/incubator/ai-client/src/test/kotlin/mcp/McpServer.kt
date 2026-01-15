package mcp

import mcp.tools.DeleteFile
import mcp.tools.EditFile
import mcp.tools.ListFiles
import mcp.tools.ReadFile
import mcp.tools.RunCommand
import org.http4k.ai.mcp.model.McpEntity
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
    RunCommand(),
).asServer(JettyLoom(port))

