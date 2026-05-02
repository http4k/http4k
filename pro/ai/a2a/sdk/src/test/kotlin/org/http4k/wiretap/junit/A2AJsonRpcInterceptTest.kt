package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.model.Role
import org.http4k.core.Uri
import org.http4k.protocol.A2A
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class A2AJsonRpcInterceptTest {

    private val message = MessageResponse.Message(Message(Role.Assistant, listOf()))

    @RegisterExtension
    val intercept = Intercept.a2aJsonRpc(Always) {
        A2A(AgentCard("name", Uri.of("http://someuri"), "0.0.0")) {
            message
        }
    }

    @Test
    fun `can pass through an a2a client`(a2AClient: A2AClient) {
        assertThat(a2AClient.message(Message(Role.User, listOf(Part.Text("foo")))), equalTo(Success(message)))
        assertThat(a2AClient.agentCard(), equalTo(null))
    }
}
