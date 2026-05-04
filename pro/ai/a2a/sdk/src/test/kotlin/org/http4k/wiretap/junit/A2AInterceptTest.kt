/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.model.A2ARole
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.core.Uri
import org.http4k.protocol.A2A
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class A2AInterceptTest {

    private val message = Message(MessageId.of("msg-1"), A2ARole.ROLE_AGENT, listOf())
    private val url = Uri.of("http://someuri/foobar")
    private val version = Version.of("1.0.0")

    @RegisterExtension
    val intercept = Intercept.a2a(Always, baseUrl = url) {
        A2A(AgentCard("name", version, "desc")) { ResponseStream(sequenceOf(message, message)) }
    }

    @Test
    fun `can pass through an a2a client`(a2AClient: A2AClient) {
        assertThat(a2AClient.agentCard(), equalTo(Success(AgentCard("name", version, "desc"))))
        val single =
            a2AClient.message(Message(MessageId.of("msg-2"), A2ARole.ROLE_USER, listOf(Part.Text("foo"))))
                .valueOrNull()
        assertThat(single, equalTo(message))

        val stream =
            a2AClient.messageStream(Message(MessageId.of("msg-2"), A2ARole.ROLE_USER, listOf(Part.Text("foo"))))
                .valueOrNull()!! as ResponseStream
        assertThat(stream.toList(), equalTo(listOf(message, message)))
    }
}
