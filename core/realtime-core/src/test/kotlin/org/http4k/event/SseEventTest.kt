package org.http4k.event

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.SseTransaction
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.events.SseEvent
import org.http4k.events.SseEvent.Outgoing
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedSseResponse
import org.http4k.sse.SseResponse
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO
import java.time.Instant

class SseEventTest {
    private val startTime = Instant.ofEpochSecond(17)
    private val tx = SseTransaction(
        request = Request(GET, ""),
        response = SseResponse(OK) {},
        start = startTime,
        duration = ZERO,
        labels = mapOf()
    )
    
    @Test
    fun `outgoing equals`() {
        assertThat(Outgoing(tx), equalTo(Outgoing(tx)))
    }

    @Test
    fun `outgoing uses template if available`() {
        assertThat(Outgoing(SseTransaction(
            request = Request(GET, "/bob"),
            response = SseResponse(OK) {},
            start = startTime,
            duration = ZERO,
            labels = mapOf()
        )).xUriTemplate, equalTo("bob"))
        assertThat(Outgoing(SseTransaction(
            request = Request(GET, "/bob"),
            response = RoutedSseResponse(SseResponse(OK) {}, UriTemplate.from("bar")),
            start = startTime,
            duration = ZERO,
            labels = mapOf()
        )).xUriTemplate, equalTo("bar"))
    }

    @Test
    fun `incoming uses template if available`() {
        assertThat(SseEvent.Incoming(SseTransaction(Request(GET, "/bob"), SseResponse(OK) {}, ZERO, mapOf(),startTime)).xUriTemplate, equalTo("bob"))
        assertThat(
            SseEvent.Incoming(
                SseTransaction(
                    request = RoutedRequest(Request(GET, "/bob"), UriTemplate.from("bar")),
                    response = SseResponse(OK) {},
                    start = startTime,
                    duration = ZERO,
                    labels = mapOf()
                )
            ).xUriTemplate, equalTo("bar"))
    }

    @Test
    fun `incoming equals`() {
        assertThat(SseEvent.Incoming(tx), equalTo(SseEvent.Incoming(tx)))
    }
}
