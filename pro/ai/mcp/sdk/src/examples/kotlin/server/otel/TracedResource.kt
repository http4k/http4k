package server.otel

import org.http4k.ai.mcp.ResourceFilter
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.then
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.routing.bind
import server.LinksOnPage

fun TracedResource(filter: (ResourceName) -> ResourceFilter): ResourceCapability {
    val name = ResourceName.of("HTTP4K")

    return filter(name).then(
        Resource.Static(Uri.of("https://www.http4k.org"), name, "description") bind LinksOnPage(
            JavaHttpClient()
        )
    )
}
