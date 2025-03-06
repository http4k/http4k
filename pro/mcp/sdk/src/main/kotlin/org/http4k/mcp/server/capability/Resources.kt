package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.util.ObservableList
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles protocol traffic for resources features and subscriptions.
 */
class Resources(list: Iterable<ResourceCapability>) : ObservableList<ResourceCapability>(list) {
    constructor(vararg list: ResourceCapability) : this(list.toList())

    private val subscriptions = ConcurrentHashMap<Pair<Uri, SessionId>, Set<(Uri) -> Unit>>()

    /**
     * Trigger all subscriptions for the given URI as it has been updated.
     */
    fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    fun listResources(req: McpResource.List.Request, http: Request) = McpResource.List.Response(
        items.map { it.toResource() }.filter { it.uri != null }
    )

    fun listTemplates(req: McpResource.Template.List.Request, http: Request) = McpResource.Template.List.Response(
        items.map { it.toResource() }.filter { it.uriTemplate != null }
    )

    fun read(req: McpResource.Read.Request, http: Request) = items
        .find { it.matches(req.uri) }
        ?.read(req, http)
        ?: throw McpException(InvalidParams)

    fun subscribe(sessionId: SessionId, req: McpResource.Subscribe.Request, fn: (Uri) -> Unit) {
        subscriptions.getOrPut(req.uri to sessionId, ::emptySet).let {
            subscriptions[req.uri to sessionId] = it + fn
        }
    }

    override fun remove(sessionId: SessionId) {
        super.remove(sessionId)
        subscriptions.keys.removeIf { it.second == sessionId }
    }

    fun unsubscribe(sessionId: SessionId, req: McpResource.Unsubscribe.Request) {
        subscriptions.remove(req.uri to sessionId)
    }
}
