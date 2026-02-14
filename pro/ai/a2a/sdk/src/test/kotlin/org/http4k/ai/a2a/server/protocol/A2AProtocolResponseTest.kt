package org.http4k.ai.a2a.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

class A2AProtocolResponseTest {

    @Test
    fun `Single wraps a single response node`() {
        val node = MoshiString("test")
        val response = A2AProtocolResponse.Single(node)
        assertThat(response.response, equalTo(node))
    }

    @Test
    fun `Stream wraps a sequence of response nodes`() {
        val nodes = listOf("a", "b", "c").map { MoshiString(it) }
        val response = A2AProtocolResponse.Stream(nodes.asSequence())
        assertThat(response.responses.toList(), equalTo(nodes))
    }
}
