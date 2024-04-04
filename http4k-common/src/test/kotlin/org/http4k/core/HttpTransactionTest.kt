package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RequestWithRoute
import org.http4k.routing.ResponseWithRoute
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.junit.jupiter.api.Test
import java.time.Duration.ZERO

class HttpTransactionTest {

    @Test
    fun `cannot get the routing group from a standard Response`() {
        assertThat(HttpTransaction(Request(GET, Uri.of("/")), Response(OK), ZERO).routingGroup, equalTo("UNMAPPED"))
    }

    @Test
    fun `can get the routing group from a RoutedResponse`() {
        val response = RoutedResponse(Response(OK), UriTemplate.from("hello"))
        assertThat(HttpTransaction(Request(GET, Uri.of("/")), response, ZERO).routingGroup, equalTo("hello"))
    }

    @Test
    fun `can create with request and response extensions`() {
        class ExtendedRequest(val delegate: RoutedRequest) : RequestWithRoute by delegate

        class ExtendedResponse(val delegate: RoutedResponse) : ResponseWithRoute by delegate

        val request = ExtendedRequest(RoutedRequest(Request(GET, "/the-path"), UriTemplate.from("request")))
        assertThat(HttpTransaction(request, Response(OK), ZERO).labels, equalTo(mapOf("routingGroup" to "request")))

        val response = ExtendedResponse(RoutedResponse(Response(OK, "/the-path"), UriTemplate.from("response")))
        assertThat(HttpTransaction(request, response, ZERO).labels, equalTo(mapOf("routingGroup" to "response")))
    }
}
