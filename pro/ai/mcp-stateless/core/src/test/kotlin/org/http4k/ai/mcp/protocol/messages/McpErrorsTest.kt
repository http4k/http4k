/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.util.McpJson
import org.junit.jupiter.api.Test

class McpErrorsTest {

    @Test
    fun `header mismatch uses -32020`() {
        assertThat(HeaderMismatchError("Mcp-Name mismatch").code, equalTo(-32020))
    }

    @Test
    fun `missing required client capability uses -32021 with a capabilities object in data`() {
        val error = MissingRequiredClientCapabilityError(listOf("sampling"))
        assertThat(error.code, equalTo(-32021))
        assertThat(
            McpJson.compact(error(McpJson)),
            containsSubstring("""{"requiredCapabilities":{"sampling":{}}}""")
        )
    }

    @Test
    fun `a missing required extension is nested under extensions in data`() {
        val error = MissingRequiredClientCapabilityError(
            requiredExtensions = listOf("io.modelcontextprotocol/tasks")
        )
        assertThat(error.code, equalTo(-32021))
        assertThat(
            McpJson.compact(error(McpJson)),
            containsSubstring("""{"requiredCapabilities":{"extensions":{"io.modelcontextprotocol/tasks":{}}}}""")
        )
    }

    @Test
    fun `unsupported protocol version uses -32022 with requested + supported in data`() {
        val error = UnsupportedProtocolVersionError(
            ProtocolVersion.of("1900-01-01"),
            listOf(ProtocolVersion.of("2026-07-28"))
        )
        assertThat(error.code, equalTo(-32022))
        val json = McpJson.compact(error(McpJson))
        assertThat(json, containsSubstring("1900-01-01"))
        assertThat(json, containsSubstring("2026-07-28"))
    }
}
