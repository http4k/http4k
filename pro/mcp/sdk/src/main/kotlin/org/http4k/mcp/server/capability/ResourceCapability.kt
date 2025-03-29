package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceWithClientHandler
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Client.Companion.NoOp

interface ResourceCapability : ServerCapability, ResourceWithClientHandler, ResourceHandler {
    fun toResource(): McpResource
    fun matches(uri: Uri): Boolean
    fun read(mcp: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response
}

fun ResourceCapability(resource: Resource, handler: ResourceHandler) =
    ResourceCapability(resource) { request, _ -> handler(request) }

fun ResourceCapability(resource: Resource, handler: ResourceWithClientHandler) = object : ResourceCapability {
    override fun toResource() = with(resource) {
        McpResource(
            if (this is Resource.Static) uri else null,
            if (this is Resource.Templated) uriTemplate else null,
            name,
            description,
            mimeType
        )
    }

    override fun matches(uri: Uri) = resource.matches(uri)

    override fun read(mcp: McpResource.Read.Request, client: Client, http: Request) =
        handler(ResourceRequest(mcp.uri, http), client).let {
            McpResource.Read.Response(it.list, it.meta)
        }

    override fun invoke(p1: ResourceRequest) = handler(p1, NoOp)
    override fun invoke(p1: ResourceRequest, client: Client) = handler(p1, client)
}
