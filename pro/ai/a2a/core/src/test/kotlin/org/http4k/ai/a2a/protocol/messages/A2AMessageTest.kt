package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.model.Role
import org.junit.jupiter.api.Test

class A2AMessageTest {

    @Test
    fun `Send Request serializes correctly`() {
        val request = A2AMessage.Send.Request(
            message = Message(
                role = Role.User,
                parts = listOf(Part.Text("Hello"))
            )
        )
        val json = A2AJson.asFormatString(request)
        assertThat(json.contains("\"role\":\"user\""), equalTo(true))
        assertThat(json.contains("\"text\":\"Hello\""), equalTo(true))
    }

    @Test
    fun `Send Request deserializes correctly`() {
        val json = """{"message":{"role":"user","parts":[{"type":"text","text":"Hello"}],"kind":"message"}}"""
        val request = A2AJson.asA<A2AMessage.Send.Request>(json)
        assertThat(request.message.role, equalTo(Role.User))
        assertThat((request.message.parts.first() as Part.Text).text, equalTo("Hello"))
    }

    @Test
    fun `Send method is message_send`() {
        assertThat(A2AMessage.Send.Method.value, equalTo("message/send"))
    }

    @Test
    fun `Stream method is message_stream`() {
        assertThat(A2AMessage.Stream.Method.value, equalTo("message/stream"))
    }
}
