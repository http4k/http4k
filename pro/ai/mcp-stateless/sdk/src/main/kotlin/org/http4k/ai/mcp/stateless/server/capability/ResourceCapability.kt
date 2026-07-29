/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.ResourceFilter
import org.http4k.ai.mcp.stateless.ResourceHandler
import org.http4k.ai.mcp.stateless.ResourceRequest
import org.http4k.ai.mcp.stateless.ResourceResponse.Error
import org.http4k.ai.mcp.stateless.ResourceResponse.InputRequired
import org.http4k.ai.mcp.stateless.ResourceResponse.Ok
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.Resource.Static
import org.http4k.ai.mcp.stateless.model.Resource.Templated
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.messages.DomainError
import org.http4k.ai.mcp.stateless.protocol.messages.McpResource
import org.http4k.ai.mcp.stateless.then
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
        when (val result = this(
            ResourceRequest(
                mcp.uri, mcp._meta, client, http, mcp.inputResponses.toElicitationResponses(), mcp.requestState
            )
        )) {
            is Ok -> McpResource.Read.Response.Result(
                result.list, ttlMs = result.ttlMs, cacheScope = resource.cacheScope, _meta = result.meta
            )

            is Error -> throw McpException(DomainError(result.message))

            is InputRequired -> McpResource.Read.Response.Result(
                emptyList(),
                resultType = ResultType.input_required,
                inputRequests = result.inputRequests.toWireRequests(mcp._meta.clientCapabilities()),
                requestState = result.requestState
            )
        }

    override fun invoke(p1: ResourceRequest) = handler(p1)
}

fun ResourceFilter.then(capability: ResourceCapability) = ResourceCapability(capability.resource, then(capability))
