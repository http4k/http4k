package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.core.Uri
import org.http4k.util.ObservableList

class Resources(list: List<ResourceBinding>) : ObservableList<ResourceBinding>(list) {

    private val subscriptions = mutableMapOf<Pair<Uri, SessionId>, Set<(Uri) -> Unit>>()

    fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    fun list(req: Resource.List.Request) = Resource.List.Response(
        items.map(ResourceBinding::toResource)
    )

    fun read(req: Resource.Read.Request) = items.find { it.uri == req.uri }
        ?.read()
        ?.let { Resource.Read.Response(it) }
        ?: error("no resource")

    fun subscribe(sessionId: SessionId, req: Resource.Subscribe.Request, fn: (Uri) -> Unit) {
        subscriptions.getOrPut(req.uri to sessionId, ::emptySet).let {
            subscriptions[req.uri to sessionId] = it + fn
        }
    }

    fun unsubscribe(sessionId: SessionId, req: Resource.Unsubscribe.Request) {
        subscriptions.remove(req.uri to sessionId)
    }
}
