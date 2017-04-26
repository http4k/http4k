package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.body.toBody

class ResponseExtensionsKtTest {
    @Test
    fun can_create_response_using_header_and_body() {
        assertThat(ok(), equalTo(Response(OK)))
        assertThat(notFound(), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun can_modify_body() {
        val testBody = "abc".toBody()
        assertThat(ok().body(testBody).body, equalTo(testBody))
    }

    @Test
    fun can_add_headers() {
        val message = ok().header("foo", "bar").header("Foo", "Bar")

        assertThat(message.headers, equalTo(listOf("foo" to "bar", "Foo" to "Bar") as Headers))
    }

    @Test
    fun can_remove_header() {
        val message = ok().header("foo", "one").header("bar", "two").removeHeader("foo")

        assertThat(message.headers, equalTo(listOf("bar" to "two") as Headers))
    }

    @Test
    fun header_removal_is_case_insensitive() {
        val message = ok().header("foo", "bar").header("Foo", "Bar").removeHeader("foo")

        assertThat(message.headers.size, equalTo(0))
    }

    @Test
    fun can_replace_header() {
        val message = ok().header("foo", "bar").header("Foo", "Bar").removeHeader("foo").replaceHeader("Foo", "replaced")

        assertThat(message.headers, equalTo(listOf("Foo" to "replaced") as Headers))
    }
}