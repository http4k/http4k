/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.util.McpJson
import org.junit.jupiter.api.Test

class McpSubscriptionsTest {

    @Test
    fun `listen request with filter round-trips`() {
        val request = McpSubscriptions.Listen.Request(
            McpSubscriptions.Listen.Request.Params(
                SubscriptionFilter(
                    toolsListChanged = true,
                    resourceSubscriptions = listOf("file:///project/config.json")
                )
            ),
            id = "1"
        )

        val json = McpJson.asFormatString(request)
        assertThat(json, containsSubstring("subscriptions/listen"))
        assertThat(json, containsSubstring("file:///project/config.json"))
        assertThat(McpJson.asA<McpSubscriptions.Listen.Request>(json), equalTo(request))
    }

    @Test
    fun `acknowledged notification round-trips with honored subset`() {
        val ack = McpSubscriptions.Acknowledged.Notification(
            McpSubscriptions.Acknowledged.Notification.Params(SubscriptionFilter(toolsListChanged = true))
        )

        val json = McpJson.asFormatString(ack)
        assertThat(json, containsSubstring("notifications/subscriptions/acknowledged"))
        assertThat(McpJson.asA<McpSubscriptions.Acknowledged.Notification>(json), equalTo(ack))
    }

    @Test
    fun `listen result carries resultType complete`() {
        val response = McpSubscriptions.Listen.Response(McpSubscriptions.Listen.Response.Result(), id = "1")

        val json = McpJson.asFormatString(response)
        assertThat(json, containsSubstring("\"resultType\":\"complete\""))
        assertThat(McpJson.asA<McpSubscriptions.Listen.Response>(json), equalTo(response))
    }
}
