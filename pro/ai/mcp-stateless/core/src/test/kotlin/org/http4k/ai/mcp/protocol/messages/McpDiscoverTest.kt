/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.TtlMs

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.CacheScope
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.util.McpJson
import org.junit.jupiter.api.Test

class McpDiscoverTest {

    @Test
    fun `request round-trips polymorphically via server-discover`() {
        val request = McpDiscover.Request(id = "1")

        val json = McpJson.asFormatString(request)
        assertThat(json, containsSubstring("server/discover"))
        assertThat(McpJson.asA<McpJsonRpcRequest>(json), equalTo(request as McpJsonRpcRequest))
    }

    @Test
    fun `result carries supportedVersions + cache hints and round-trips`() {
        val response = McpDiscover.Response(
            McpDiscover.Response.Result(
                supportedVersions = listOf(ProtocolVersion.of("2026-07-28")),
                ttlMs = TtlMs.of(3_600_000),
                cacheScope = CacheScope.public
            ),
            id = "1"
        )

        val json = McpJson.asFormatString(response)
        assertThat(json, containsSubstring("2026-07-28"))
        assertThat(json, containsSubstring("public"))
        assertThat(McpJson.asA<McpDiscover.Response>(json), equalTo(response))
    }
}
