package org.http4k.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.http4k.http.core.body.toBody

class HttpMessageAsStringTest {
    @Test
    fun represents_request_to_string() {
        val request = Request(Method.GET, Uri.uri("http://www.somewhere.com/path"), listOf("foo" to "one", "bar" to "two"), "body".toBody())
        assertThat(request.toString(), equalTo("""
        GET http://www.somewhere.com/path HTTP/1.1
        foo: one
        bar: two

        body""".trimIndent().replace("\n", "\r\n")))
    }

    @Test
    fun represents_response_to_string() {
        val request = Response(Status.OK, listOf("foo" to "one", "bar" to "two"), "body".toBody())
        assertThat(request.toString(), equalTo("""
        HTTP/1.1 200 OK
        foo: one
        bar: two

        body""".trimIndent().replace("\n", "\r\n")))
    }
}