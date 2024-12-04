package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO
import java.time.Instant

class HttpEventTest {
    private val startTime = Instant.ofEpochSecond(17)
    private val tx = HttpTransaction(
        request = Request(GET, ""),
        response = Response(OK),
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
        assertThat(Outgoing(HttpTransaction(
            request = Request(GET, "/bob"),
            response = Response(OK),
            start = startTime,
            duration = ZERO,
            labels = mapOf()
        )).xUriTemplate, equalTo("bob"))
        assertThat(Outgoing(HttpTransaction(
            request = Request(GET, "/bob"),
            response = RoutedResponse(Response(OK), UriTemplate.from("bar")),
            start = startTime,
            duration = ZERO,
            labels = mapOf()
        )).xUriTemplate, equalTo("bar"))
    }

    @Test
    fun `incoming uses template if available`() {
        assertThat(Incoming(HttpTransaction(Request(GET, "/bob"), Response(OK), ZERO, mapOf(),startTime)).xUriTemplate, equalTo("bob"))
        assertThat(
            Incoming(
                HttpTransaction(
                    request = RoutedRequest(Request(GET, "/bob"), UriTemplate.from("bar")),
                    response = Response(OK),
                    start = startTime,
                    duration = ZERO,
                    labels = mapOf()
                )
            ).xUriTemplate, equalTo("bar"))
    }

    @Test
    fun `incoming equals`() {
        assertThat(Incoming(tx), equalTo(Incoming(tx)))
    }

    @Test
    fun `toString is working as expected`() {
        assertThat(Incoming(tx).toString(), equalTo("Incoming(uri=, method=GET, status=200 OK, latency=0, xUriTemplate=, protocol=http)"))
        assertThat(Outgoing(tx).toString(), equalTo("Outgoing(uri=, method=GET, status=200 OK, latency=0, xUriTemplate=, protocol=http)\""))
    }
}
