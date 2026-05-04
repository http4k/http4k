/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.testing.toHttpHandler

/**
 * Create a test A2A JSON-RPC client from a PolyHandler, dispatching SSE requests
 * to the SSE handler and all other requests to the HTTP handler.
 */
fun PolyHandler.testA2AJsonRpcClient(baseUri: Uri = Uri.of("")): A2AClient =
    HttpA2AClient(baseUri, http = toHttpHandler())

/**
 * Create a test A2A REST client from a PolyHandler, dispatching SSE requests
 * to the SSE handler and all other requests to the HTTP handler.
 */
fun PolyHandler.testA2ARestClient(baseUri: Uri = Uri.of("")): A2AClient =
    RestA2AClient(baseUri, http = toHttpHandler())
