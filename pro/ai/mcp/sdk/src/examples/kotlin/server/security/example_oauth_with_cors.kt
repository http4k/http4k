package server.security

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.then
import org.http4k.filter.AnyOf
import org.http4k.filter.CorsAndRebindProtection
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.PolyFilters
import org.http4k.filter.debug
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * This example demonstrates how to protect an MCP server with CORs protection against cross origin and
 * DNS rebind. Note that there is no security added here - you can see the other examples for this,
 */
fun main() {
    val secureMcpServer = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        NoMcpSecurity,
    )

    // declare a CORs policy which is used to
    PolyFilters.CorsAndRebindProtection(
        CorsPolicy(
            OriginPolicy.AnyOf("foo.com"),
            listOf("allowed-header"), listOf(GET, POST, DELETE)
        )
    )
        .then(secureMcpServer)
        .debug(debugStream = true)
        .asServer(JettyLoom(3001))
        .start()
}
