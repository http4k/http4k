/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.ToolsChanged
import org.http4k.ai.mcp.protocol.Version
import org.junit.jupiter.api.Test

class DiscoverTest {

    @Test
    fun `advertises the server's supported versions, capabilities and instructions`() {
        val metaData = ServerMetaData(
            McpEntity.of("test-server"),
            Version.of("1.0.0"),
            ToolsChanged,
            instructions = "how to use me",
            protocolVersions = setOf(ProtocolVersion.of("2026-07-28"))
        )

        val result = discoverResultFor(metaData)

        assertThat(result.supportedVersions, equalTo(listOf(ProtocolVersion.of("2026-07-28"))))
        assertThat(result.instructions, equalTo("how to use me"))
        assertThat(result.capabilities, equalTo(metaData.capabilities))
    }
}
