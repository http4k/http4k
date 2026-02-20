package org.http4k.ai.mcp.apps.endpoints

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.McpServerResult.Failure
import org.http4k.ai.mcp.apps.McpServerResult.Success
import org.http4k.ai.mcp.apps.McpServerResult.Unknown
import org.http4k.ai.mcp.apps.ResourceResponse
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.bind

fun ReadUiResource(servers: McpApps) =
    "/api/resources" bind GET to {
        val serverUri = Query.required("serverId")(it)
        val uiResource = Query.uri().required("uri")(it)

        when (val resource = servers.render(serverUri, uiResource)) {
            is Success<ResourceResponse> -> {
                val response = Response(OK).body(resource.value.content)
                resource.value.csp?.toHeaderValue()?.let { response.header("Content-Security-Policy", it) } ?: response
            }

            is Failure -> Response(INTERNAL_SERVER_ERROR).body(it.toString())

            Unknown -> Response(BAD_REQUEST).body("Server not found")
        }
    }

private fun Csp.toHeaderValue(): String = listOfNotNull(
    resourceDomains?.let { "default-src ${it.joinToString(" ")}" },
    connectDomains?.let { "connect-src ${it.joinToString(" ")}" },
    frameDomains?.let { "frame-src ${it.joinToString(" ")}" },
    baseUriDomains?.let { "base-uri ${it.joinToString(" ")}" }
).joinToString("; ")
