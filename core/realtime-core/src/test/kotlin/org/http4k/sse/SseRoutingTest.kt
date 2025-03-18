package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.orElse
import org.http4k.routing.query
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test

class SseRoutingTest {

    @Test
    fun `uses router for matching`() {
        val app = sse(
            query("goodbye") bind sse {
                it.send(SseMessage.Data("query"))
                it.close()
            },
            orElse bind sse {
                it.send(SseMessage.Data("vanilla"))
                it.close()
            }
        )

        assertThat(
            app.testSseClient(Request(GET, "/").query("goodbye", "bob")).received().toList(), equalTo(
                listOf(SseMessage.Data("query"))
            )
        )
        assertThat(
            app.testSseClient(Request(GET, "/")).received().toList(), equalTo(
                listOf(SseMessage.Data("vanilla"))
            )
        )
    }
}
