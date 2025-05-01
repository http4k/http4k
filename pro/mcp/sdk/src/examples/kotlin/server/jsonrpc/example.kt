package server.jsonrpc

import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.filter.debug
import org.http4k.lens.contentType
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpJsonRpc
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.tools

/**
 * This example demonstrates how to create an MCP server using the JSONRPC protocol.
 */
fun main() {
    val mcpServer = mcpJsonRpc(
        ServerMetaData(McpEntity.of("http4k mcp via jsonrpc"), Version.of("0.1.0")),
        prompts(),
        resources(),
        tools(),
        completions()
    )

    mcpServer.debug().asServer(JettyLoom(3001)).start()

    // you can use straight HTTP to interact with the server
    JavaHttpClient().debug()(
        Request(POST, "http://localhost:3001/jsonrpc")
            .contentType(APPLICATION_JSON)
            .body("""{"jsonrpc":"2.0","method":"tools/list","params":{"_meta":{}},"id":1}""")
    )
}
