/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class CapabilitiesExtensionsTest {

    @Test
    fun `ClientCapabilities declares an extension`() {
        val capabilities = ClientCapabilities().withExtensions(TASKS to emptyMap<String, Any>())

        assertThat(capabilities.extensions, equalTo(mapOf<String, Any>(TASKS to emptyMap<String, Any>())))
    }

    @Test
    fun `ClientCapabilities declaring a second extension keeps the first`() {
        val capabilities = ClientCapabilities()
            .withExtensions(TASKS to emptyMap<String, Any>())
            .withExtensions(UI to emptyMap<String, Any>())

        assertThat(capabilities.extensions?.keys, equalTo(setOf(TASKS, UI)))
    }

    @Test
    fun `ClientCapabilities with extensions`(approver: Approver) {
        val extensions = mapOf(
            "io.modelcontextprotocol/ui" to mapOf(
                "mimeTypes" to listOf("text/html;profile=mcp-app")
            )
        )
        val capabilities = ClientCapabilities().copy(extensions = extensions)

        approver.assertApproved(McpJson.asFormatString(capabilities), APPLICATION_JSON)
    }

    @Test
    fun `ServerCapabilities with extensions`(approver: Approver) {
        val extensions = mapOf(
            "io.modelcontextprotocol/ui" to mapOf(
                "mimeTypes" to listOf("text/html;profile=mcp-app")
            )
        )
        val capabilities = ServerCapabilities().copy(extensions = extensions)

        approver.assertApproved(McpJson.asFormatString(capabilities), APPLICATION_JSON)
    }
}

private const val TASKS = "io.modelcontextprotocol/tasks"
private const val UI = "io.modelcontextprotocol/ui"
