package org.http4k.ai.mcp.apps

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates

data class ToolOption(
    val serverId: String,
    val serverName: String,
    val toolName: ToolName,
    val resourceUri: Uri
)

fun McpAppsHost(servers: List<Uri>, http: HttpHandler = JavaHttpClient()): RoutingHttpHandler {
    val serverClients = servers.associateWith { HttpNonStreamingMcpClient(it, http) }

    val tools = serverClients
        .mapNotNull { (serverUri, client) ->
            client.tools().list()
                .map {
                    it
                        .mapNotNull { tool ->
                            tool._meta?.ui?.resourceUri
                                ?.let { tool.name to it }
                        }
                        .map { ToolOption(serverUri.toString(), serverUri.host, it.first, it.second) }
                }.valueOrNull()
        }.flatten()

    val renderer = HandlebarsTemplates().CachingClasspath()

    return routes(
        "/api/resources" bind GET to {
            val serverUri = Query.uri().required("serverId")(it)
            val uiResource = Query.uri().required("uri")(it)

            Response(OK)
                .body(
                    when (val client = serverClients[serverUri]) {
                        null -> "Server not found"
                        else -> client.resources().read(ResourceRequest(uiResource))
                            .map { it.list.filterIsInstance<Text>().joinToString("") { it.text.replace("\"", "'") } }
                            .recover { it.toString() }
                    })
        },

        "/api/tools/call" bind POST to {

            println(it.bodyString())

            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body("""{"content": [{"type": "text", "text": "dummy tool result"}]}""")
        },

        "/" bind GET to { Response(OK).body(renderer(Index(tools))) }
    )
}

