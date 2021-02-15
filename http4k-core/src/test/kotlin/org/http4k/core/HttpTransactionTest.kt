package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
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
}
