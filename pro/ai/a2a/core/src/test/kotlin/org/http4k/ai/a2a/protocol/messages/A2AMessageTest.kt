/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.protocol.A2ARpcMethod
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.model.Role
import org.junit.jupiter.api.Test

class A2AMessageTest {

    @Test
    fun `Send Request serializes correctly`() {
        val request = A2AMessage.Send.Request(
            params = A2AMessage.Send.Request.Params(
                message = Message(
                    role = Role.User,
                    parts = listOf(Part.Text("Hello"))
                )
            ),
            id = "1"
        )
        val json = A2AJson.asFormatString(request)
        assertThat(json.contains("\"role\":\"user\""), equalTo(true))
        assertThat(json.contains("\"text\":\"Hello\""), equalTo(true))
    }

    @Test
    fun `Send Request deserializes correctly`() {
        val json = """{"params":{"message":{"role":"user","parts":[{"type":"text","text":"Hello"}],"kind":"message"}},"id":"1","jsonrpc":"2.0","method":"SendMessage"}"""
        val request = A2AJson.asA<A2AMessage.Send.Request>(json)
        assertThat(request.params.message.role, equalTo(Role.User))
        assertThat((request.params.message.parts.first() as Part.Text).text, equalTo("Hello"))
    }

    @Test
    fun `Send method is message_send`() {
        assertThat(
            A2AMessage.Send.Request(
                params = A2AMessage.Send.Request.Params(
                    message = Message(role = Role.User, parts = emptyList())
                ),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("SendMessage"))
        )
    }

    @Test
    fun `Stream method is message_stream`() {
        assertThat(
            A2AMessage.Stream.Request(
                params = A2AMessage.Stream.Request.Params(
                    message = Message(role = Role.User, parts = emptyList())
                ),
                id = "1"
            ).method, equalTo(A2ARpcMethod.of("SendStreamingMessage"))
        )
    }
}
