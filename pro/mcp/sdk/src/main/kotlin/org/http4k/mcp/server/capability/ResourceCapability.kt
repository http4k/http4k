package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Resource.Static
import org.http4k.mcp.model.Resource.Templated
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.server.protocol.Client

interface ResourceCapability : ServerCapability, ResourceHandler {
    fun toResource(): McpResource
    fun matches(uri: Uri): Boolean
    fun read(mcp: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

fun ResourceCapability(resource: Resource, handler: ResourceHandler) = object : ResourceCapability {
    override fun toResource() = with(resource) {
        McpResource(
            if (this is Static) uri else null,
            if (this is Templated) uriTemplate else null,
            name,
            description,
            mimeType,
            size,
            annotations
        )
    }

    override fun matches(uri: Uri) = resource.matches(uri)

    override fun read(mcp: McpResource.Read.Request, client: Client, http: Request) =
        handler(ResourceRequest(mcp.uri, mcp._meta, client, http)).let {
            McpResource.Read.Response(it.list, it.meta)
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}
