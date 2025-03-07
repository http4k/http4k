package websocket

import org.http4k.filter.debug
import org.http4k.mcp.McpDesktop
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.bind
import org.http4k.routing.mcpWebsocket
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.time.Instant

fun main() {
    val mcpServer = mcpWebsocket(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        Tool("time", "Get the current time") bind { ToolResponse.Ok(listOf(Content.Text(Instant.now().toString()))) }
    )

    mcpServer
        .debug()
        .asServer(Helidon(5001))
        .start()

    McpDesktop.main("--transport", "websocket", "--url", "ws://localhost:5001/ws")

    println("""Now paste the MCP JSON-RPC requests into the console eg. {"jsonrpc":"2.0","method":"tools/call","params":{"name":"time","arguments":{},"_meta":{}}}""")
}

