/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.ai.mcp.stateless.util.McpJson
import org.junit.jupiter.api.Test

class JsonRpcIdTest {

    private fun roundTrip(id: String) = McpJson.asFormatString(
        McpJson.asA<McpJsonRpcRequest>(
            """{"jsonrpc":"2.0","id":$id,"method":"tasks/get","params":{"taskId":"t1"}}"""
        )
    )

    @Test
    fun `a numeric id is echoed back as the same integer`() {
        assertThat(roundTrip("8"), containsSubstring(""""id":8,"""))
    }

    @Test
    fun `a string id is echoed back unchanged`() {
        assertThat(roundTrip("\"abc\""), containsSubstring(""""id":"abc","""))
    }

    @Test
    fun `a genuinely fractional value is not made integral`() {
        assertThat(
            McpJson.asFormatString(
                McpJson.asA<McpJsonRpcRequest>(
                    """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"t","arguments":{"ratio":1.5}}}"""
                )
            ),
            containsSubstring(""""ratio":1.5""")
        )
    }
}
