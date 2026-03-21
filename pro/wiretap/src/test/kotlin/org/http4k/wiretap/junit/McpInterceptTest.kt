/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.acceptance.orThrowIt
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import wiretap.examples.McpServerWithOtelTracing

class McpInterceptTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val intercept = Intercept(downstream, Always) {
        McpServerWithOtelTracing(http(), otel("test app 1")).http!!
    }

    @Test
    fun `can pass through an mcp client`(mcpClient: McpClient) {
        assertThat(mcpClient.tools().list().orThrowIt().size, greaterThan(0))
        assertThat(mcpClient.prompts().list().orThrowIt().size, greaterThan(0))
    }
}
