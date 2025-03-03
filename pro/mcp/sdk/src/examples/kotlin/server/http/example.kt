package server.http

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.sampling
import server.tools

fun main() {
    val mcpServer = mcpHttp(
        ServerMetaData(
            McpEntity.of("http4k mcp via HTTP"), Version.of("0.1.0"),
            *ProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        sampling(),
        completions()
    )

    mcpServer.asServer(Helidon(3001)).start()
}

