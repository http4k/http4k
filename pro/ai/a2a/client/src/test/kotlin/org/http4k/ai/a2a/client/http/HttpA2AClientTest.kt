/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client.http

import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.client.A2AClientContract
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

class HttpA2AClientTest : A2AClientContract() {
    override fun clientFor(server: HttpHandler) = A2AClient.Http(Uri.of("http://test"), server)
}
