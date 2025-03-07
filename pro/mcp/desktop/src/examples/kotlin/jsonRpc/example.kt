package jsonRpc

import org.http4k.mcp.McpDesktop
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.bind
import org.http4k.routing.mcpJsonRpc
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.time.Instant

fun main() {
    val mcpServer = mcpJsonRpc(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        Tool("time", "Get the current time") bind { ToolResponse.Ok(listOf(Content.Text(Instant.now().toString()))) }
    )

    mcpServer
        .asServer(Helidon(3001))
        .start()

    McpDesktop.main("--transport", "jsonrpc", "--url", "http://localhost:3001/jsonrpc", "--debug")

    println("""Now paste the MCP JSON-RPC requests into the console eg. {"jsonrpc":"2.0","method":"tools/call","params":{"name":"time","arguments":{},"_meta":{}}}""")
}
