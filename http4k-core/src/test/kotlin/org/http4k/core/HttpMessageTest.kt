package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpMessageTest {

    @Test
    fun can_modify_body() {
        val testBody = "abc".toBody()
        assertThat(Response(Status.OK).body(testBody).body, equalTo(testBody))
    }

    @Test
    fun can_add_headers() {
        val message = Response(Status.OK).header("foo", "bar").header("Foo", "Bar")

        assertThat(message.headers, equalTo(listOf("foo" to "bar", "Foo" to "Bar") as Headers))
    }

    @Test
    fun can_remove_header() {
        val message = Response(Status.OK).header("foo", "one").header("bar", "two").removeHeader("foo")

        assertThat(message.headers, equalTo(listOf("bar" to "two") as Headers))
    }

    @Test
    fun header_removal_is_case_insensitive() {
        val message = Response(Status.OK).header("foo", "bar").header("Foo", "Bar").removeHeader("foo")

        assertThat(message.headers.size, equalTo(0))
    }

    @Test
    fun can_replace_header() {
        val message = Response(Status.OK).header("foo", "bar").header("Foo", "Bar").removeHeader("foo").replaceHeader("Foo", "replaced")

        assertThat(message.headers, equalTo(listOf("Foo" to "replaced") as Headers))
    }
}