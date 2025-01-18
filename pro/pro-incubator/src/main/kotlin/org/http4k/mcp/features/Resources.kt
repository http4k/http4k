package org.http4k.mcp.features

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.server.SessionId
import org.http4k.routing.ResourceFeatureBinding
import org.http4k.util.ObservableList

/**
 * Handles protocol traffic for resources features and subscriptions.
 */
class Resources(list: List<ResourceFeatureBinding>) : ObservableList<ResourceFeatureBinding>(list), McpFeature {

    private val subscriptions = mutableMapOf<Pair<Uri, SessionId>, Set<(Uri) -> Unit>>()

    /**
     * Trigger all subscriptions for the given URI as it has been updated.
     */
    fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    fun listResources(req: McpResource.List.Request, http: Request) = McpResource.List.Response(
        items
            .map { it.toResource() }
            .filterIsInstance<Resource.Static>()
    )

    fun listTemplates(req: McpResource.Template.List.Request, http: Request) = McpResource.Template.List.Response(
        items.map(ResourceFeatureBinding::toResource)
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
