package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.http4k.core.Body
import org.http4k.core.HttpMessage
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.UriTemplate.Companion.from
import org.junit.jupiter.api.Test

class RoutedMessageTest {
    private val template = from("an-uri-template")

    @Test
    fun `request manipulations maintain the same type`() {
        val request = RoutedRequest(Request(GET, "/"), template)

        assertThat(request.method(POST), isA<RoutedRequest>())
        assertThat(request.uri(Uri.of("/changed")), isA<RoutedRequest>())
        assertThat(request.query("foo", "bar"), isA<RoutedRequest>())
        assertThat(request.headers(listOf("foo" to "bar")), isA<RoutedRequest>())

        checkMessageFields<RoutedRequest>(request)
    }

    @Test
    fun `response manipulations maintain the same type`() {
        val response = RoutedResponse(Response(NOT_FOUND), template)

        checkMessageFields<RoutedResponse>(response)
    }

    private inline fun <reified T : Any> checkMessageFields(request: HttpMessage) {
        assertThat(request.header("foo", "bar"), isA<T>())
        assertThat(request.replaceHeader("foo", "bar"), isA<T>())
        assertThat(request.removeHeader("foo"), isA<T>())
        assertThat(request.body("foo"), isA<T>())
        assertThat(request.body(Body.EMPTY), isA<T>())
        assertThat(request.body("foo".byteInputStream()), isA<T>())
    }
}
