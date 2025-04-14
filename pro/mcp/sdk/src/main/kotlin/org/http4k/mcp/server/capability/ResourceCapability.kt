package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.Client
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Resource.Static
import org.http4k.mcp.model.Resource.Templated
import org.http4k.mcp.protocol.messages.McpResource

interface ResourceCapability : ServerCapability, ResourceHandler {
    fun toResource(): McpResource
    fun matches(uri: Uri): Boolean
    fun read(mcp: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

fun ResourceCapability(resource: Resource, handler: ResourceHandler) = object : ResourceCapability {
    override fun toResource() = with(resource) {
        when (this) {
            is Static -> McpResource(uri, name, description, mimeType, size, annotations)
            is Templated -> McpResource(uriTemplate, name, description, mimeType, size, annotations)
        }
    }

    override fun matches(uri: Uri) = resource.matches(uri)

    override fun read(mcp: McpResource.Read.Request, client: Client, http: Request) =
        handler(ResourceRequest(mcp.uri, mcp._meta, client, http)).let {
            McpResource.Read.Response(it.list, it.meta)
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}
