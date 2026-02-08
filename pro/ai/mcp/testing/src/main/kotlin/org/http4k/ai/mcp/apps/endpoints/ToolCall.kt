package org.http4k.ai.mcp.apps.endpoints

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.model.HostToolRequest
import org.http4k.ai.mcp.apps.model.HostToolResponse
import org.http4k.ai.mcp.apps.util.McpAppsJson.auto
import org.http4k.ai.mcp.apps.McpServerResult.Failure
import org.http4k.ai.mcp.apps.McpServerResult.Success
import org.http4k.ai.mcp.apps.McpServerResult.Unknown
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun ToolCall(servers: McpApps): RoutingHttpHandler =
    "/api/tools/call" bind POST to {
        val toolCall = Body.auto<HostToolRequest>().toLens()(it)

        Response(OK)
            .with(
                Body.auto<HostToolResponse>().toLens() of when (val result = servers.callTool(toolCall)) {
                    is Success<HostToolResponse> -> result.value
                    is Failure -> HostToolResponse(listOf(Text(result.reason)))
                    Unknown -> HostToolResponse(listOf(Text("No such server")))
                }
            )
    }


