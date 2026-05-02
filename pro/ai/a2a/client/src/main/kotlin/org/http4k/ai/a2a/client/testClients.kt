/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * Create a test A2A client from an HttpHandler.
 */
fun HttpHandler.testHttpClient(): A2AClient = HttpA2AClient(Uri.of(""), this)

/**
 * Create a test A2A client from an HttpHandler.
 */
fun HttpHandler.testA2ARestClient(): A2AClient = RestA2AClient(Uri.of(""), this)
