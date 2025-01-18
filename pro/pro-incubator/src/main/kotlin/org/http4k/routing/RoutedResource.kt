package org.http4k.routing

import org.http4k.core.Request
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.McpResource

class RoutedResource(val resource: Resource, val handler: ResourceHandler) : McpRouting {
    fun toResource() = resource

    fun read(http: Request) = handler(ResourceRequest(resource.uri, http)).let {
        McpResource.Read.Response(it.list, it.meta)
    }
}
