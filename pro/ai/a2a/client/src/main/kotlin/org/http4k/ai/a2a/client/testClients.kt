/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.core.PolyHandler
import org.http4k.core.Uri

/**
 * Create a test A2A client from an PolyHandler.
 */
fun PolyHandler.testA2AJsonRpcClient(baseUri: Uri = Uri.of("")): A2AClient = HttpA2AClient(baseUri, http!!)

/**
 * Create a test A2A client from an PolyHandler.
 */
fun PolyHandler.testA2ARestClient(baseUri: Uri = Uri.of("")): A2AClient = RestA2AClient(baseUri, http!!)
