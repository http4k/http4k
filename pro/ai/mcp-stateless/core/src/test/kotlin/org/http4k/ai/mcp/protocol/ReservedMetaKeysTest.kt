/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.clientInfo
import org.http4k.lens.protocolVersion
import org.http4k.lens.serverInfo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ReservedMetaKeysTest {

    private val protocolVersion = MetaKey.protocolVersion().toLens()
    private val clientCapabilities = MetaKey.clientCapabilities().toLens()
    private val clientInfo = MetaKey.clientInfo().toLens()
    private val serverInfo = MetaKey.serverInfo().toLens()

    private val version = ProtocolVersion.of("2026-07-28")
    private val info = VersionedMcpEntity(McpEntity.of("ExampleClient"), Version.of("1.0.0"))

    @Test
    fun `protocolVersion roundtrip via _meta`() {
        val meta = Meta(protocolVersion of version)
        assertThat(protocolVersion(meta), equalTo(version))
    }

    @Test
    fun `reserved keys use the io_modelcontextprotocol prefix on the wire`() {
        val meta = Meta(protocolVersion of version)
        assertThat(meta["io.modelcontextprotocol/protocolVersion"], present())
    }

    @Test
    fun `client capabilities + client info serialization roundtrip`() {
        val caps = ClientCapabilities()
        val meta = Meta(clientCapabilities of caps, clientInfo of info)
        val roundTripped = McpJson.asA<Meta>(McpJson.asFormatString(meta))
        assertThat(clientCapabilities(roundTripped), equalTo(caps))
        assertThat(clientInfo(roundTripped), equalTo(info))
    }

    @Test
    fun `server info roundtrip via result _meta`() {
        val meta = Meta(serverInfo of info)
        assertThat(serverInfo(meta), equalTo(info))
    }

    @Test
    @Disabled("FIXME")
    fun `missing required field returns null`() {
        assertThat(protocolVersion(Meta()), absent())
    }
}
