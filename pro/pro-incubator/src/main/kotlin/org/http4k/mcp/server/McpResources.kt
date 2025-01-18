package org.http4k.mcp.server

import org.http4k.connect.mcp.McpResource
import org.http4k.core.Uri
import org.http4k.mcp.SessionId
import org.http4k.routing.RoutedResource
import org.http4k.util.ObservableList

class McpResources(list: List<RoutedResource>) : ObservableList<RoutedResource>(list) {

    private val subscriptions = mutableMapOf<Pair<Uri, SessionId>, Set<(Uri) -> Unit>>()

    fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    fun list(req: McpResource.List.Request) = McpResource.List.Response(
        items.map(RoutedResource::toResource)
    )

    fun read(req: McpResource.Read.Request) = items.find { it.uri == req.uri }
        ?.read()
        ?.let { McpResource.Read.Response(
            it) }
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
