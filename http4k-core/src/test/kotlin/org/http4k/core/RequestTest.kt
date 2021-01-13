package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET

import org.junit.jupiter.api.Test

class RequestTest {
    @Test
    fun `extracts query parameters`() {
        assertThat(requestWithQuery("foo=one&foo=two").queries("foo"), equalTo(listOf<String?>("one", "two")))
        assertThat(requestWithQuery("foo=one&foo=two").query("foo")!!, equalTo("one"))
        assertThat(requestWithQuery("foo=one&foo&foo=two").queries("foo"), equalTo(listOf("one", null, "two")))
    }

    @Test
    fun `differences in header order do not invalidate equality`() {
        val requestOne = Request(GET, "http://ignore").header("foo", "bar").header("fizz", "buzz")
        val requestTwo = Request(GET, "http://ignore").header("fizz", "buzz").header("foo", "bar")
        assertThat(requestOne, equalTo(requestTwo))
    }

    @Test
    fun `if multiple headers with the same key exist in two headers, they must be in the same order for the headers to be equal`() {
        val requestOne = Request(GET, "http://ignore")
            .header("foo", "bar")
            .header("foo", "buzz")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("Content-Type", "application/json")
        val requestTwo = Request(GET, "http://ignore")
            .header("foo", "buzz")
            .header("foo", "bar")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("Content-Type", "application/json")
        val requestThree = Request(GET, "http://ignore")
            .header("Content-Type", "application/json")
            .header("fizz", "bar")
            .header("fizz", "buzz")
            .header("foo", "buzz")
            .header("foo", "bar")

        assertThat(requestOne, !equalTo(requestTwo))
        assertThat(requestTwo, equalTo(requestThree))
    }

    private fun requestWithQuery(query: String) = Request(GET, Uri.of("http://ignore/?$query"))

    @Test
    fun `request has default src ip and port`() {
        val request = Request(GET, "http://ignore")
        assert(request.source == null)
    }

    @Test
    fun `request has modifiable src ip and port`() {
        val request = Request(GET, "http://ignore")
            .source(RequestSource("192.168.0.1", 32768))
        assertThat(request.source, equalTo(RequestSource("192.168.0.1", 32768)))
    }
}



