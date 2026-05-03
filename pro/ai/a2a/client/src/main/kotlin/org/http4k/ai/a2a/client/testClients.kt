/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.core.ContentType
import org.http4k.core.PolyHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.testing.PolyHandlerTestClient

/**
 * Create a test A2A JSON-RPC client from a PolyHandler, dispatching SSE requests
 * to the SSE handler and all other requests to the HTTP handler.
 */
fun PolyHandler.testA2AJsonRpcClient(baseUri: Uri = Uri.of("")): A2AClient {
    val polyClient = PolyHandlerTestClient(this)

    return HttpA2AClient(baseUri, http = { request ->
        if (request.header("Accept")?.contains("text/event-stream") == true) {
            val sseClient = polyClient.sse(request)
            val body = sseClient.received().joinToString("") { it.toMessage() }
            Response(sseClient.status)
                .header("content-type", ContentType.TEXT_EVENT_STREAM.toHeaderValue())
                .body(body)
        } else {
            polyClient.http(request)
        }
    })
}

/**
 * Create a test A2A REST client from a PolyHandler.
 */
fun PolyHandler.testA2ARestClient(baseUri: Uri = Uri.of("")): A2AClient = RestA2AClient(baseUri, http!!)
