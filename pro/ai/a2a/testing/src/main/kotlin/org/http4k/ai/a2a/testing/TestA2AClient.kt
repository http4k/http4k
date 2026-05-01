/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.testing

import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.client.http.HttpA2AClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * Test client for A2A protocol. Useful for testing A2A servers in-memory.
 */
class TestA2AClient(http: HttpHandler) : A2AClient by HttpA2AClient(Uri.of(""), http)

/**
 * Create a test A2A client from an HttpHandler.
 */
fun HttpHandler.testA2AClient() = TestA2AClient(this)
