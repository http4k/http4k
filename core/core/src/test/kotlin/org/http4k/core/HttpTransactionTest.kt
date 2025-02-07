package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RequestWithContext
import org.http4k.routing.ResponseWithContext
import org.http4k.routing.RoutedMessage
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO
import java.time.Instant

class HttpTransactionTest {
    private val startTime = Instant.ofEpochSecond(93)

    @Test
    fun `cannot get the routing group from a standard Response`() {
        assertThat(
            HttpTransaction(
                request = Request(GET, Uri.of("/")),
                response = Response(OK),
                start = startTime,
                duration = ZERO
            ).routingGroup, equalTo("UNMAPPED")
        )
    }

    @Test
    fun `can get the routing group from a RoutedResponse`() {
        val response = ResponseWithContext(Response(OK), UriTemplate.from("hello"))
        assertThat(
            HttpTransaction(
                request = Request(GET, Uri.of("/")),
                response = response,
                start = startTime,
                duration = ZERO
            ).routingGroup, equalTo("hello")
        )
    }

    @Test
    fun `can create with request and response extensions`() {
        class ExtendedRequest(val delegate: RequestWithContext) : Request by delegate, RoutedMessage by delegate

        class ExtendedResponse(val delegate: ResponseWithContext) : Response by delegate, RoutedMessage by delegate

        val request = ExtendedRequest(RequestWithContext(Request(GET, "/the-path"), UriTemplate.from("request")))
        assertThat(
            HttpTransaction(
                request = request,
                response = Response(OK),
                start = startTime,
                duration = ZERO
            ).labels, equalTo(mapOf("routingGroup" to "request"))
        )

        val response = ExtendedResponse(ResponseWithContext(Response(OK, "/the-path"), UriTemplate.from("response")))
        assertThat(
            HttpTransaction(
                request = request,
                response = response,
                start = startTime,
                duration = ZERO
            ).labels, equalTo(mapOf("routingGroup" to "response"))
        )
    }
}
