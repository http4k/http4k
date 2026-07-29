/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceFilter
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse.Error
import org.http4k.ai.mcp.ResourceResponse.Ok
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.Resource.Templated
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.then
import org.http4k.core.Request
import org.http4k.core.Uri

class ResourceCapability(
    internal val resource: Resource,
    internal val handler: ResourceHandler
) : ServerCapability, ResourceHandler {

    override val name = resource.name.value

    fun toResource() = with(resource) {
        when (this) {
            is Static -> McpResource(uri, name, description, mimeType, size, annotations, title, icons, meta ?: Meta.default)
            is Templated -> McpResource(uriTemplate, name, description, mimeType, size, annotations, title, icons, meta ?: Meta.default)
        }
    }

    fun matches(uri: Uri) = resource.matches(uri)

    fun read(mcp: McpResource.Read.Request.Params, client: Client, http: Request) =
        when (val result = this(ResourceRequest(mcp.uri, mcp._meta, client, http))) {
            is Ok -> McpResource.Read.Response.Result(result.list, result.meta)
            is Error -> throw McpException(DomainError(result.message))
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}

fun ResourceFilter.then(capability: ResourceCapability) = ResourceCapability(capability.resource, then(capability))
