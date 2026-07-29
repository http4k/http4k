/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.MoshiObject
import org.http4k.lens.MetaKey
import org.http4k.lens.serverInfo
import org.junit.jupiter.api.Test

class ServerInfoDecoratorTest {

    private val serverInfo = VersionedMcpEntity(McpEntity.of("ExampleServer"), Version.of("1.0.0"))
    private val serverInfoLens = MetaKey.serverInfo().toLens()

    @Test
    fun `merges serverInfo into a result's _meta`() {
        val node = McpJson.parse("""{"jsonrpc":"2.0","id":1,"result":{"tools":[]}}""")

        val decorated = node.withServerInfo(serverInfo)

        val resultMeta = (decorated as MoshiObject).result()._meta()
        assertThat(serverInfoLens(resultMeta), equalTo(serverInfo))
    }

    @Test
    fun `preserves other _meta keys already on the result`() {
        val node = McpJson.parse("""{"jsonrpc":"2.0","id":1,"result":{"tools":[],"_meta":{"custom":"kept"}}}""")

        val decorated = node.withServerInfo(serverInfo)

        val resultMeta = (decorated as MoshiObject).result()._meta()
        assertThat(serverInfoLens(resultMeta), equalTo(serverInfo))
        assertThat(resultMeta["custom"], equalTo(McpJson.parse(""""kept"""")))
    }

    @Test
    fun `leaves an error response (no result) untouched`() {
        val node = McpJson.parse("""{"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"nope"}}""")

        assertThat(node.withServerInfo(serverInfo), equalTo(node))
    }

    private fun MoshiObject.result() = attributes["result"] as MoshiObject
    private fun MoshiObject._meta() = Meta(attributes["_meta"] as MoshiObject)
}
