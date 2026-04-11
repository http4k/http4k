/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.apps

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.McpClientFactory
import org.http4k.connect.model.MimeType
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcp
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class McpAppsHostTest {
    private val appName = "mcp app"
    private val uiUrl = Uri.of("ui://a-ui")

    private fun McpApp(): PolyHandler = mcp(
        ServerMetaData("mcp app", "0.0.0").withExtensions(McpApps),
        NoMcpSecurity,
        RenderMcpApp("show_ui", "shows the UI", uiUrl, emptyList()) {
            ResourceResponse.Ok(
                Resource.Content.Text(
                    "hello world", uiUrl, MimeType.IMAGE_PNG, Content.Meta(
                        McpAppResourceMeta(
                            Csp(
                                listOf(Domain.of("https://connect.com")),
                                listOf(Domain.of("https://resource.com")),
                                listOf(Domain.of("https://frame.com"))
                            )
                        )
                    )
                )
            )
        },
    )

    private fun NonMcpApp() = mcp(
        ServerMetaData("mcp server", "0.0.0").withExtensions(McpApps),
        NoMcpSecurity,
        Tool("non_app", "") bind { ToolResponse.Ok("hello") }
    )

    private val host = McpAppsHost(
        McpClientFactory.Test(McpApp()),
        McpClientFactory.Test(NonMcpApp())
    )

    @Test
    fun `renders the UI on startup`(approver: Approver) {
        approver.assertApproved(host(Request(GET, "/")))
    }

    @Test
    fun `gets the UI for the resource`(approver: Approver) {
        approver.assertApproved(
            host(
                Request(GET, "/api/resources").query("serverId", appName).query("uri", uiUrl.toString())
            )
        )
    }

    @Test
    fun `sets CSP header from resource response`() {
        val response = host(
            Request(GET, "/api/resources").query("serverId", appName).query("uri", uiUrl.toString())
        )

        assertThat(
            response.header("Content-Security-Policy"),
            equalTo("default-src https://resource.com; connect-src https://connect.com; frame-src https://frame.com")
        )
    }
}
