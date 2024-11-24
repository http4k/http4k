package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.sse.bind
import org.http4k.sse.SseResponse
import org.junit.jupiter.api.Test

class SseRoutingTest {

    @Test
    fun `can route to method`() {
        setOf(GET, POST).forEach {
            val sseClient = sse("/routeMethod/{name}" bind sse(
                POST to { SseResponse(OK, listOf("METHOD" to it.method.name + it.path("name"))) { it.close() } },
                GET to { SseResponse(OK, listOf("METHOD" to it.method.name + it.path("name"))) { it.close() } }
            ))

            val response = sseClient(Request(it, "/routeMethod/foo"))

            assertThat(response.status, equalTo(OK))
            assertThat(response.headers, equalTo(listOf("METHOD" to "${it.name}foo")))
        }
    }
}
