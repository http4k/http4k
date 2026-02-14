package org.http4k.ai.a2a.server.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.server.util.toSseStream
import org.http4k.format.MoshiString
import org.junit.jupiter.api.Test

class SseStreamingTest {

    @Test
    fun `toSseStream converts sequence of nodes to SSE format`() {
        val nodes = sequenceOf(
            MoshiString("event1"),
            MoshiString("event2"),
            MoshiString("event3")
        )

        val inputStream = nodes.toSseStream()
        val result = inputStream.bufferedReader().readText()

        assertThat(result, containsSubstring("data: \"event1\""))
        assertThat(result, containsSubstring("data: \"event2\""))
        assertThat(result, containsSubstring("data: \"event3\""))
    }

    @Test
    fun `toSseStream produces events separated by double newlines`() {
        val nodes = sequenceOf(
            MoshiString("a"),
            MoshiString("b")
        )

        val inputStream = nodes.toSseStream()
        val result = inputStream.bufferedReader().readText()

        val events = result.split("\n\n").filter { it.isNotEmpty() }
        assertThat(events.size, equalTo(2))
    }
}
