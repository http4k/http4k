package demo

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.McpFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.PolyFilters
import org.http4k.routing.mcp
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri

fun McpDemo(http: HttpHandler = JavaHttpClient()): PolyHandler {
    val identity = ServerMetaData("Demo", "0.0.0")

    val httpClient: HttpHandler = ClientFilters.OpenTelemetryTracing().then(http)

    val capabilities: ServerCapability = CountdownApp(httpClient)

    val mcpServer: PolyHandler =
        mcp(identity, NoMcpSecurity, capabilities, mcpFilter = McpFilters.OpenTelemetryTracing())

    return PolyFilters.OpenTelemetryTracing().then(mcpServer)
}

fun main() {
    val app = McpDemo(EventsServer())
    val url = app.asServer(Jetty(12345)).start().uri()

    println("MCP server running at: ${url.path("/mcp")}")
}
