package org.http4k.mcp

import org.http4k.connect.mcp.Complete
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.Initialize
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.core.Body
import org.http4k.format.jsonRpcRequest
import org.http4k.routing.sse
import org.http4k.sse.SseHandler

fun McpHandler(
    implementation: Implementation,
    capabilities: ServerCapabilities,
    tools: Tools,
    resources: Resources,
    prompts: Prompts,
): SseHandler {
    val serDe = ServerResponseSerde(McpJson)

    return sse {
        val params = Body.jsonRpcRequest(McpJson).toLens()(it.connectRequest)

        when (McpRpcMethod.of(params.method)) {
            Initialize.Method -> it.send(serDe(initialize(serDe(params))))
            Ping.Method -> ping()

            Complete.Method -> complete(serDe(params))

            Prompt.Get.Method -> prompts.get(serDe(params))
            Prompt.List.Method -> prompts.list(serDe(params))

            Resource.List.Method -> resources.list(serDe(params))
            Resource.Read.Method -> resources.read(serDe(params))
            Resource.Subscribe.Method -> resources.subscribe(serDe(params))
            Resource.Unsubscribe.Method -> resources.unsubscribe(serDe(params))

            Tool.Call.Method -> tools.call(serDe(params))
            Tool.List.Method -> tools.list(serDe(params))

            else -> throw IllegalArgumentException("Unknown method: ${params.method}")
        }
    }
}

fun complete(convert: Complete.Request): Complete.Response {
    TODO()
}

fun ping() {
    TODO()
}

fun initialize(convert: Initialize.Request): Initialize.Response {
    TODO()
}
