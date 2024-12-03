package org.http4k.event

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.core.WsTransaction
import org.http4k.events.WsEvent
import org.http4k.events.WsEvent.Outgoing
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedWsResponse
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.BUGGYCLOSE
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO
import java.time.Instant

class WsEventTest {
    private val startTime = Instant.ofEpochSecond(17)
    private val tx = WsTransaction(
        request = Request(GET, ""),
        response = WsResponse("sub") {},
        status = BUGGYCLOSE,
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
        assertThat(
            Outgoing(
                WsTransaction(
                    request = Request(GET, "/bob"),
                    response = WsResponse("sub") {},
                    status = BUGGYCLOSE,
                    start = startTime,
                    duration = ZERO,
                    labels = mapOf()
                )
            ).xUriTemplate, equalTo("bob")
        )
        assertThat(
            Outgoing(
                WsTransaction(
                    request = Request(GET, "/bob"),
                    response = RoutedWsResponse(WsResponse("sub") {}, UriTemplate.from("bar")),
                    status = BUGGYCLOSE,
                    start = startTime,
                    duration = ZERO,
                    labels = mapOf()
                )
            ).xUriTemplate, equalTo("bar")
        )
    }

    @Test
    fun `incoming uses template if available`() {
        assertThat(
            WsEvent.Incoming(
                WsTransaction(
                    Request(GET, "/bob"),
                    WsResponse("sub") {},
                    status = BUGGYCLOSE,
                    ZERO,
                    mapOf(),
                    startTime
                )
            ).xUriTemplate, equalTo("bob")
        )
        assertThat(
            WsEvent.Incoming(
                WsTransaction(
                    request = RoutedRequest(Request(GET, "/bob"), UriTemplate.from("bar")),
                    response = WsResponse("sub") {},
                    status = BUGGYCLOSE,
                    start = startTime,
                    duration = ZERO,
                    labels = mapOf()
                )
            ).xUriTemplate, equalTo("bar")
        )
    }

    @Test
    fun `incoming equals`() {
        assertThat(WsEvent.Incoming(tx), equalTo(WsEvent.Incoming(tx)))
    }
}
