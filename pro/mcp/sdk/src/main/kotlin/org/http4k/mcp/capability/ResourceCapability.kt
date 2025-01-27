package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.messages.McpResource

class ResourceCapability(private val resource: Resource, val handler: ResourceHandler) : ServerCapability {

    fun toResource() = with(resource) {
        McpResource(
            if (this is Resource.Static) uri else null,
            if (this is Resource.Templated) uriTemplate else null,
            name,
            description,
            mimeType
        )
    }

    fun matches(uri: Uri) = resource.matches(uri)

    fun read(mcp: McpResource.Read.Request, http: Request) = handler(ResourceRequest(mcp.uri, http)).let {
        McpResource.Read.Response(it.list, it.meta)
    }
}
