package wiretap.examples

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp

fun McpApp() = mcp(
    ServerMetaData("mcp app", "0.0.0").withExtensions(McpApps),
    NoMcpSecurity,
    RenderMcpApp(
        name = "show_ui",
        description = "shows the UI",
        uri = Uri.of("ui://a-ui"),
        meta = McpAppResourceMeta(
            csp = Csp(
                resourceDomains = listOf(Domain.of("https://resource.com")),
                connectDomains = listOf(Domain.of("https://connect.com")),
                frameDomains = listOf(Domain.of("https://frame.com"))
            )
        )
    ) { "hello world" },
    Tool("standard_tool", "") bind { Ok("hello") },
)
