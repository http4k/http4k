package demo

import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.extension.McpAppViewModelResourceHandler
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import java.time.Clock

fun CountdownApp(http: HttpHandler): ServerCapability {
    val resourceHandler = McpAppViewModelResourceHandler(
        HandlebarsTemplates().CachingClasspath(),
        McpAppResourceMeta(csp = Csp(resourceDomains = listOf(Domain.of("unpkg.com"))))
    ) { CountdownUI() }

    return RenderMcpApp(
        name = "show_ui",
        description = "shows the MCP App UI",
        capabilities = listOf(
            GetCountdown(Clock.systemUTC()),
            GetRandomEvent(ClientFilters.SetBaseUriFrom(Uri.of("https://events-calendar")).then(http))
        ),
        resourceHandler = resourceHandler
    )
}

class CountdownUI : ViewModel
