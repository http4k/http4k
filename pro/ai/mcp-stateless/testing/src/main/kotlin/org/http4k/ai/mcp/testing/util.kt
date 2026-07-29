/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.client.McpClient

fun McpClient.useClient(fn: McpClient.() -> Unit) {
    use {
        it.start()
        it.fn()
    }
}
