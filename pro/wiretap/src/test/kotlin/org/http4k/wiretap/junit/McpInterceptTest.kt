/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.coerce
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.reverseProxy
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import wiretap.examples.McpServerWithOtelTracing

class McpInterceptTest {

    @RegisterExtension
    @JvmField
    val intercept = Intercept.poly(Always) {
        McpServerWithOtelTracing(
            reverseProxy(
                "http4k" to http { Response(OK).body("downstream") },
                "spring" to http { Response(INTERNAL_SERVER_ERROR).body("downstream") }
            ), otel("test app 1"))
    }

    @Test
    fun `can pass through an mcp client`(mcpClient: McpClient) {
        mcpClient.run {
            assertThat(
                resources().read(ResourceRequest(Uri.of("ui://a-ui"))).coerce<ResourceResponse.Ok>().list.first().uri,
                equalTo(Uri.of("ui://a-ui"))
            )
            assertThat(tools().list().coerce<List<McpTool>>().size, greaterThan(0))
            assertThat(prompts().list().coerce<List<McpPrompt>>().size, greaterThan(0))
        }
    }
}
