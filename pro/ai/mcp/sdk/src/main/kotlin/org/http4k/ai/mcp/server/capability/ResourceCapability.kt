package org.http4k.ai.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.Resource.Templated
import org.http4k.ai.mcp.protocol.messages.McpResource

interface ResourceCapability : ServerCapability, ResourceHandler {
    fun toResource(): McpResource
    fun matches(uri: Uri): Boolean
    fun read(mcp: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

fun ResourceCapability(resource: Resource, handler: ResourceHandler) = object : ResourceCapability {
    override fun toResource() = with(resource) {
        when (this) {
            is Static -> McpResource(uri, name, description, mimeType, size, annotations, title, icons)
            is Templated -> McpResource(uriTemplate, name, description, mimeType, size, annotations, title, icons)
        }
    }

    override fun matches(uri: Uri) = resource.matches(uri)

    override fun read(mcp: McpResource.Read.Request, client: Client, http: Request) =
        this(ResourceRequest(mcp.uri, mcp._meta, client, http)).let {
            McpResource.Read.Response(it.list, it.meta)
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}
