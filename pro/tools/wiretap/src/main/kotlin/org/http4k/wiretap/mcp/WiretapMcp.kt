package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.PolyHandler
import org.http4k.routing.mcpHttpStreaming
import org.http4k.wiretap.WiretapFunction

fun WiretapMcp(
    name: String,
    mcpSecurity: McpSecurity,
    functions: List<WiretapFunction>,
): PolyHandler = mcpHttpStreaming(
    ServerMetaData(name, "0.0.0"),
    mcpSecurity,
    *(
        listOf(AnalyzeTrafficPrompt(), DebugRequestPrompt()) +
            functions.map { it.mcp() }).toTypedArray()
)
