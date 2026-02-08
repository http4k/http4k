package org.http4k.ai.mcp.apps.endpoints

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.McpServerResult.Failure
import org.http4k.ai.mcp.apps.McpServerResult.Success
import org.http4k.ai.mcp.apps.McpServerResult.Unknown
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.bind

fun ReadUiResource(servers: McpApps) =
    "/api/resources" bind GET to {
        val serverUri = Query.required("serverId")(it)
        val uiResource = Query.uri().required("uri")(it)

        Response(OK)
            .body(
                when (val r = servers.render(serverUri, uiResource)) {
                    is Success<String> -> r.value
                    is Failure -> it.toString()
                    Unknown -> "Server not found"
                }
            )
    }
