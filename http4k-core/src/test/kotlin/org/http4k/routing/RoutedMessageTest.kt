package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.core.Body
import org.http4k.core.HttpMessage
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.UriTemplate.Companion.from
import org.junit.jupiter.api.Test

class RoutedMessageTest {
    private val template = from("an-uri-template")

    @Test
    fun `routed request can be extended`() {
        class ExtendedRequest(val delegate: RoutedRequest) : RequestWithRoute by delegate {
            override fun query(name: String, value: String?): ExtendedRequest =
                ExtendedRequest(delegate.query(name, value) as RoutedRequest)
        }

        val request = ExtendedRequest(RoutedRequest(Request(GET, "/the-path"), from("/{pathParam}")))

        assertThat(request.path("pathParam"), equalTo("the-path"))
        assertThat(request.query("name","value"), isA<ExtendedRequest>())

        checkMessageFields<RoutedRequest>(request)
    }

    @Test
    fun `routed response can be extended`() {
        class ExtendedResponse(val delegate: RoutedResponse) : ResponseWithRoute by delegate {
            override fun header(name: String, value: String?): ExtendedResponse =
                ExtendedResponse(delegate.header(name, value) as RoutedResponse)
        }

        val response = ExtendedResponse(RoutedResponse(Response(OK, "/the-path"), from("/{pathParam}")))

        assertThat(response.header("name","value"), isA<ExtendedResponse>())
    }

    @Test
    fun `request manipulations maintain the same type`() {
        val request = RoutedRequest(Request(GET, "/"), template)

        assertThat(request.method(POST), isA<RoutedRequest>())
        assertThat(request.uri(Uri.of("/changed")), isA<RoutedRequest>())
        assertThat(request.query("foo", "bar"), isA<RoutedRequest>())
        assertThat(request.headers(listOf("foo" to "bar")), isA<RoutedRequest>())
        assertThat(request.removeQuery("foo"), isA<RoutedRequest>())
        assertThat(request.removeQueries("foo"), isA<RoutedRequest>())
        assertThat(request.source(RequestSource("localhost")), isA<RoutedRequest>())

        checkMessageFields<RoutedRequest>(request)
    }

    @Test
    fun `response manipulations maintain the same type`() {
        val response = RoutedResponse(Response(NOT_FOUND), template)

        assertThat(response.status(OK), isA<RoutedResponse>())

        checkMessageFields<RoutedResponse>(response)
    }

    private inline fun <reified T : Any> checkMessageFields(request: HttpMessage) {
        assertThat(request.header("foo", "bar"), isA<T>())
        assertThat(request.headers(listOf("foo" to "bar")), isA<T>())
        assertThat(request.replaceHeader("foo", "bar"), isA<T>())
        assertThat(request.replaceHeaders(listOf("foo" to "bar")), isA<T>())
        assertThat(request.removeHeader("foo"), isA<T>())
        assertThat(request.removeHeaders("foot"), isA<T>())
        assertThat(request.body("foo"), isA<T>())
        assertThat(request.body(Body.EMPTY), isA<T>())
        assertThat(request.body("foo".byteInputStream()), isA<T>())
    }
}
