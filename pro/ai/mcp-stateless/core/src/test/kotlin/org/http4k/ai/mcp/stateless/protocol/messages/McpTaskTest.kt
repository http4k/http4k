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
class McpTaskTest {

    private val taskId = "786512e2-9e0d-44bd-8f29-789f320fe840"

    @Test
    fun `a tasks-get request`(approver: Approver) = approver.roundTrips<McpTask.Get.Request>(
        """{"jsonrpc":"2.0","id":8,"method":"tasks/get","params":{"taskId":"$taskId"}}"""
    )

    @Test
    fun `a tasks-update request carrying responses to outstanding input requests`(approver: Approver) =
        approver.roundTrips<McpTask.Update.Request>(
            """{"jsonrpc":"2.0","id":6,"method":"tasks/update","params":{"taskId":"$taskId",""" +
                """"inputResponses":{"name":{"action":"accept","content":{"input":"Luca"}}}}}"""
        )

    @Test
    fun `a tasks-cancel request`(approver: Approver) = approver.roundTrips<McpTask.Cancel.Request>(
        """{"jsonrpc":"2.0","id":9,"method":"tasks/cancel","params":{"taskId":"$taskId"}}"""
    )

    @Test
    fun `a tool call answering with a task handle instead of a result`(approver: Approver) =
        approver.roundTrips<McpTool.Call.Response>(
            """{"jsonrpc":"2.0","id":1,"result":{"resultType":"task","taskId":"$taskId",""" +
                """"status":"working","statusMessage":"The operation is now in progress.",""" +
                """"createdAt":"2025-11-25T10:30:00Z","lastUpdatedAt":"2025-11-25T10:40:00Z",""" +
                """"ttlMs":60000,"pollIntervalMs":5000}}"""
        )

    @Test
    fun `a completed task carrying the original result`(approver: Approver) =
        approver.roundTrips<McpTask.Get.Response>(
            """{"jsonrpc":"2.0","id":8,"result":{"resultType":"complete","taskId":"$taskId",""" +
                """"status":"completed","createdAt":"2025-11-25T10:30:00Z",""" +
                """"lastUpdatedAt":"2025-11-25T10:50:00Z","ttlMs":3600000,"pollIntervalMs":5000,""" +
                """"result":{"content":[{"type":"text","text":"Hello, Luca!"}],"isError":false}}}"""
        )

    @Test
    fun `a failed task carrying the protocol error`(approver: Approver) =
        approver.roundTrips<McpTask.Get.Response>(
            """{"jsonrpc":"2.0","id":8,"result":{"resultType":"complete","taskId":"$taskId",""" +
                """"status":"failed","createdAt":"2025-11-25T10:30:00Z",""" +
                """"lastUpdatedAt":"2025-11-25T10:50:00Z","ttlMs":null,""" +
                """"error":{"code":-32603,"message":"Internal error"}}}"""
        )

    @Test
    fun `an ordinary tool result carries no task handle`(approver: Approver) =
        approver.assertApproved(
            McpJson.asFormatString(McpTool.Call.Response.Result(isError = false)), APPLICATION_JSON
        )
}

private inline fun <reified T : Any> Approver.roundTrips(json: String) =
    assertApproved(McpJson.asFormatString(McpJson.asA<T>(json)), APPLICATION_JSON)
