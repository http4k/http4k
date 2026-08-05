/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.model.CacheScope
import org.http4k.ai.mcp.model.CacheScope.public
import org.http4k.ai.mcp.model.TtlMs
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.ResourceNotFoundError
import org.http4k.ai.mcp.server.protocol.Resources
import org.http4k.ai.mcp.util.ObservableList
import org.http4k.core.Request
import org.http4k.core.Uri
import java.util.concurrent.ConcurrentHashMap

fun resources(vararg resources: ResourceCapability, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Resources =
    resources(resources.toList(), ttlMs, cacheScope)

fun resources(list: Iterable<ResourceCapability>, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Resources =
    InMemoryResources(list, ttlMs, cacheScope)

private class InMemoryResources(
    list: Iterable<ResourceCapability>,
    private val ttlMs: TtlMs,
    private val cacheScope: CacheScope,
) : ObservableList<ResourceCapability>(list), Resources {

    // per-URI update subscriptions, keyed by the physical listen connection (see ObservableList observers)
    private val updateSubscribers = ConcurrentHashMap<Any, Pair<Set<String>, (Uri) -> Unit>>()

    override fun triggerUpdated(uri: Uri) =
        updateSubscribers.values.forEach { (uris, handler) -> if (uri.toString() in uris) handler(uri) }

    override fun subscribeToUpdates(key: Any, uris: Set<String>, handler: (Uri) -> Unit) {
        updateSubscribers[key] = uris to handler
    }

    override fun removeUpdateSubscriber(key: Any) {
        updateSubscribers.remove(key)
    }

    override fun invoke(p1: ResourceRequest) = items
        .find { it.matches(p1.uri) }
        ?.invoke(p1)
        ?: throw McpException(ResourceNotFoundError(p1.uri))

    override fun listResources(req: McpResource.List.Request.Params, client: Client, http: Request) =
        McpResource.List.Response.Result(
            items.map { it.toResource() }.filter { it.uri != null }, ttlMs = ttlMs, cacheScope = cacheScope
        )

    override fun listTemplates(req: McpResource.ListTemplates.Request.Params, client: Client, http: Request) =
        McpResource.ListTemplates.Response.Result(
            items.map { it.toResource() }.filter { it.uriTemplate != null }, ttlMs = ttlMs, cacheScope = cacheScope
        )

    override fun read(req: McpResource.Read.Request.Params, client: Client, http: Request) = items
        .find { it.matches(req.uri) }
        ?.read(req, client, http)
        ?: throw McpException(ResourceNotFoundError(req.uri))
}
