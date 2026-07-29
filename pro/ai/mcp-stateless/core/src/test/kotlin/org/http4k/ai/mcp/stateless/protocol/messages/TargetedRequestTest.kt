/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class TargetedRequestTest {

    @Test
    fun `a tools-call request`(approver: Approver) = approver.roundTrips<McpTool.Call.Request>(
        """{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"my_tool","arguments":{"a":"b"}}}"""
    )

    @Test
    fun `a prompts-get request`(approver: Approver) = approver.roundTrips<McpPrompt.Get.Request>(
        """{"jsonrpc":"2.0","id":"1","method":"prompts/get","params":{"name":"my_prompt","arguments":{"a":"b"}}}"""
    )

    @Test
    fun `a resources-read request`(approver: Approver) = approver.roundTrips<McpResource.Read.Request>(
        """{"jsonrpc":"2.0","id":"1","method":"resources/read","params":{"uri":"https://example.com/thing.txt"}}"""
    )
}

private inline fun <reified T : Any> Approver.roundTrips(json: String) =
    assertApproved(McpJson.asFormatString(McpJson.asA<T>(json)), APPLICATION_JSON)
