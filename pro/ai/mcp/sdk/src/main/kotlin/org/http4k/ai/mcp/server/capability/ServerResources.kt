/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.server.protocol.ObservableResources
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.util.ObservableList
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import java.util.concurrent.ConcurrentHashMap

fun resources(vararg resources: ResourceCapability): ObservableResources = resources(resources.toList())

fun resources(list: Iterable<ResourceCapability>): ObservableResources = ServerResources(list)

private class ServerResources(list: Iterable<ResourceCapability>) : ObservableList<ResourceCapability>(list),
    ObservableResources {

    private val subscriptions = ConcurrentHashMap<Pair<Uri, Session>, Set<(Uri) -> Unit>>()

    override fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    override fun invoke(p1: ResourceRequest) = items
        .find { it.matches(p1.uri) }
        ?.invoke(p1)
        ?: throw McpException(InvalidParams)

    override fun listResources(req: McpResource.List.Request, client: Client, http: Request) =
        McpResource.List.Response(
            items.map { it.toResource() }.filter { it.uri != null }
        )

    override fun listTemplates(req: McpResource.ListTemplates.Request, client: Client, http: Request) =
        McpResource.ListTemplates.Response(
            items.map { it.toResource() }.filter { it.uriTemplate != null }
        )

    override fun read(req: McpResource.Read.Request, client: Client, http: Request) = items
        .find { it.matches(req.uri) }
        ?.read(req, client, http)
        ?: throw McpException(InvalidParams)

    override fun subscribe(session: Session, req: McpResource.Subscribe.Request, fn: (Uri) -> Unit) {
        subscriptions.getOrPut(req.uri to session, ::emptySet).let {
            subscriptions[req.uri to session] = it + fn
        }
    }

    override fun unsubscribe(session: Session, req: McpResource.Unsubscribe.Request) {
        subscriptions.remove(req.uri to session)
    }

    override fun remove(session: Session) {
        super<ObservableList>.remove(session)
        subscriptions.keys.removeIf { it.second == session }
    }
}


