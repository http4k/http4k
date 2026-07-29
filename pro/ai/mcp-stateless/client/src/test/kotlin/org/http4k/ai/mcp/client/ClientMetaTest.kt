/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class ClientMetaTest {

    @Test
    fun `each request self-describes via reserved _meta`() {
        val captured = AtomicReference<String>()
        val client = HttpMcpClient(
            Uri.of("/mcp"),
            entity = McpEntity.of("my-client"),
            version = Version.of("9.9.9"),
            http = { req -> captured.set(req.bodyString()); Response(OK).body("""{"jsonrpc":"2.0","id":"1","result":{"tools":[]}}""") }
        )

        client.tools().list()

        val body = captured.get()
        assertThat(body, containsSubstring("io.modelcontextprotocol/protocolVersion"))
        assertThat(body, containsSubstring(LATEST_VERSION.value))
        assertThat(body, containsSubstring("io.modelcontextprotocol/clientInfo"))
        assertThat(body, containsSubstring("my-client"))
        assertThat(body, containsSubstring("io.modelcontextprotocol/clientCapabilities"))
        assertThat(body, containsSubstring("elicitation"))
    }
}
