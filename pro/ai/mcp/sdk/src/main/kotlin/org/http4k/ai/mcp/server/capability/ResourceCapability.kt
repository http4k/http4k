package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceFilter
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.Resource.Templated
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.then
import org.http4k.core.Request
import org.http4k.core.Uri

class ResourceCapability(
    internal val resource: Resource,
    internal val handler: ResourceHandler
) : ServerCapability, ResourceHandler {
    fun toResource() = with(resource) {
        when (this) {
            is Static -> McpResource(uri, name, description, mimeType, size, annotations, title, icons, meta)
            is Templated -> McpResource(uriTemplate, name, description, mimeType, size, annotations, title, icons, meta)
        }
    }

    fun matches(uri: Uri) = resource.matches(uri)

    fun read(mcp: McpResource.Read.Request, client: Client, http: Request) =
        this(ResourceRequest(mcp.uri, mcp._meta, client, http)).let {
            McpResource.Read.Response(it.list, it.meta)
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}

fun ResourceFilter.then(capability: ResourceCapability) = ResourceCapability(capability.resource, then(capability))
