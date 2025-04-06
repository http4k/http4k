package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Resources
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.util.ObservableList
import java.util.concurrent.ConcurrentHashMap


class ServerResources(list: Iterable<ResourceCapability>) : ObservableList<ResourceCapability>(list), Resources {
    constructor(vararg list: ResourceCapability) : this(list.toList())

    private val subscriptions = ConcurrentHashMap<Pair<Uri, Session>, Set<(Uri) -> Unit>>()

    /**
     * Trigger all subscriptions for the given URI as it has been updated.
     */
    override fun triggerUpdated(uri: Uri) {
        subscriptions.filterKeys { it.first == uri }.forEach { (uri, _), callbacks ->
            callbacks.forEach { it(uri) }
        }
    }

    override fun listResources(req: McpResource.List.Request, client: Client, http: Request) = McpResource.List.Response(
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
        super.remove(session)
        subscriptions.keys.removeIf { it.second == session }
    }
}
