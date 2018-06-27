package org.http4k.routing

import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Method.GET
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

        request.method(Method.POST).shouldMatch(isA<RoutedRequest>())
        request.uri(Uri.of("/changed")).shouldMatch(isA<RoutedRequest>())
        request.query("foo", "bar").shouldMatch(isA<RoutedRequest>())
        request.headers(listOf("foo" to "bar")).shouldMatch(isA<RoutedRequest>())

        checkMessageFields<RoutedRequest>(request)
    }

    @Test
    fun `response manipulations maintain the same type`() {
        val response = RoutedResponse(Response(NOT_FOUND), template)

        checkMessageFields<RoutedResponse>(response)
    }

    private inline fun <reified T : Any> checkMessageFields(request: HttpMessage) {
        request.header("foo", "bar").shouldMatch(isA<T>())
        request.replaceHeader("foo", "bar").shouldMatch(isA<T>())
        request.removeHeader("foo").shouldMatch(isA<T>())
        request.body("foo").shouldMatch(isA<T>())
        request.body(Body.EMPTY).shouldMatch(isA<T>())
        request.body("foo".byteInputStream()).shouldMatch(isA<T>())
    }
}