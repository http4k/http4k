package org.http4k.routing

import org.http4k.core.Request
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.McpResource

class ResourceFeatureBinding(private val resource: Resource, val handler: ResourceHandler) : FeatureBinding {
    fun toResource() = resource

    fun read(mcp: McpResource.Read.Request, http: Request) = handler(ResourceRequest(mcp.uri, http)).let {
        McpResource.Read.Response(it.list, it.meta)
    }
}
