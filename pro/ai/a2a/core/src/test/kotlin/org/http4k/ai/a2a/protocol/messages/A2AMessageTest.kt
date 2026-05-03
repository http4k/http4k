/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.A2ARole
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.protocol.A2ARpcMethod
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class A2AMessageTest {

    @Test
    fun `Send Request roundtrips correctly`(approver: Approver) {
        val request = A2AMessage.Send.Request(
            params = A2AMessage.Send.Request.Params(
                message = Message(
                    messageId = MessageId.of("msg-1"),
                    role = A2ARole.ROLE_USER,
                    parts = listOf(Part.Text("Hello"))
                )
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2AMessage.Send.Request>(json), equalTo(request))
    }

    @Test
    fun `Stream Request roundtrips correctly`(approver: Approver) {
        val request = A2AMessage.Stream.Request(
            params = A2AMessage.Stream.Request.Params(
                message = Message(
                    messageId = MessageId.of("msg-2"),
                    role = A2ARole.ROLE_USER,
                    parts = listOf(Part.Text("Hello streaming"))
                )
            ),
            id = "2"
        )
        val json = A2AJson.asFormatString(request)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2AMessage.Stream.Request>(json), equalTo(request))
    }

    @Test
    fun `Send method is SendMessage`() {
        assertThat(
            A2AMessage.Send.Request(
                params = A2AMessage.Send.Request.Params(
                    message = Message(messageId = MessageId.of("msg-1"), role = A2ARole.ROLE_USER, parts = emptyList())
                ),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("SendMessage"))
        )
    }

    @Test
    fun `Stream method is SendStreamingMessage`() {
        assertThat(
            A2AMessage.Stream.Request(
                params = A2AMessage.Stream.Request.Params(
                    message = Message(messageId = MessageId.of("msg-1"), role = A2ARole.ROLE_USER, parts = emptyList())
                ),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("SendStreamingMessage"))
        )
    }
}
