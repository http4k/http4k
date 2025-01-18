package org.http4k.mcp.server

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.McpResource
import org.http4k.routing.RoutedResource
import org.http4k.util.ObservableList

class McpResources(list: List<RoutedResource>) : ObservableList<RoutedResource>(list) {

    private val subscriptions = mutableMapOf<Pair<Uri, SessionId>, Set<(Uri) -> Unit>>()

    fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    fun listResources(req: McpResource.List.Request) = McpResource.List.Response(
        items
            .map { it.toResource() }
            .filterIsInstance<Resource.Static>()
    )

    fun listTemplates(req: McpResource.Template.List.Request) = McpResource.Template.List.Response(
        items.map(RoutedResource::toResource)
            .filterIsInstance<Resource.Templated>()
    )

    fun read(req: McpResource.Read.Request, http: Request) = items
        .find { it.toResource().matches(req.uri) }
        ?.read(req.uri, http)
        ?: error("no resource")

    fun subscribe(sessionId: SessionId, req: McpResource.Subscribe.Request, fn: (Uri) -> Unit) {
        subscriptions.getOrPut(req.uri to sessionId, ::emptySet).let {
            subscriptions[req.uri to sessionId] = it + fn
        }
    }

    fun unsubscribe(sessionId: SessionId, req: McpResource.Unsubscribe.Request) {
        subscriptions.remove(req.uri to sessionId)
    }
}
