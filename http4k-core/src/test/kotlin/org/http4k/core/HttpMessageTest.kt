package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class HttpMessageTest {

    @Test
    fun can_modify_body() {
        val testBody = Body("abc")
        assertThat(Response(OK).body(testBody).body, equalTo(testBody))
    }

    @Test
    fun can_modify_status() {
        assertThat(Response(OK).status(INTERNAL_SERVER_ERROR), hasStatus(INTERNAL_SERVER_ERROR))
    }

    @Test
    fun can_add_headers() {
        val message = Response(OK).header("foo", "bar").header("Foo", "Bar")

        assertThat(message.headers, equalTo(listOf("foo" to "bar", "Foo" to "Bar") as Headers))
    }

    @Test
    fun can_remove_header() {
        val message = Response(OK).header("foo", "one").header("bar", "two").removeHeader("foo")

        assertThat(message.headers, equalTo(listOf("bar" to "two") as Headers))
    }

    @Test
    fun header_removal_is_case_insensitive() {
        val message = Response(OK).header("foo", "bar").header("Foo", "Bar").removeHeader("foo")

        assertThat(message.headers.size, equalTo(0))
    }

    @Test
    fun can_replace_header() {
        val message = Response(OK).header("foo", "bar").header("Foo", "Bar").removeHeader("foo").replaceHeader("Foo", "replaced")

        assertThat(message.headers, equalTo(listOf("Foo" to "replaced") as Headers))
    }

    @Test
    fun can_replace_headers() {
        val source = Response(OK).header("Foo", "replaced")
        val message = Response(OK).header("Foo", "Bar").replaceHeaders(source.headers)

        assertThat(message.headers, equalTo(listOf("Foo" to "replaced") as Headers))
    }

    @Test
    fun `multiple headers with different cases are all retreived`() {
        fun checkHeaderInsensitivity(request: HttpMessage) {
            val req = request
                .header("foo", "one")
                .header("Foo", "two")
                .header("FOO", "three")

            assertThat(req.headerValues("foo"), equalTo(listOf<String?>("one", "two", "three")))
        }

        checkHeaderInsensitivity(Request(GET, "http://ignore"))
        checkHeaderInsensitivity(Response(OK))
    }
}
