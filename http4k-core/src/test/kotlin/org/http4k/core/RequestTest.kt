package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request.Companion.delete
import org.http4k.core.Request.Companion.get
import org.http4k.core.Request.Companion.options
import org.http4k.core.Request.Companion.patch
import org.http4k.core.Request.Companion.post
import org.http4k.core.Request.Companion.put
import org.http4k.core.Request.Companion.trace
import org.junit.Test

class RequestTest {

    @Test
    fun extracts_query_parameters() {
        assertThat(requestWithQuery("foo=one&foo=two").queries("foo"), equalTo(listOf<String?>("one", "two")))
        assertThat(requestWithQuery("foo=one&foo=two").query("foo")!!, equalTo("one"))
        assertThat(requestWithQuery("foo=one&foo&foo=two").queries("foo"), equalTo(listOf("one", null, "two")))
    }

    @Test
    fun can_create_using_method_and_uri_string() {
        assertThat(get("/uri"), equalTo(Request(GET, Uri.of("/uri"))))
        assertThat(post("/uri"), equalTo(Request(Method.POST, Uri.of("/uri"))))
        assertThat(put("/uri"), equalTo(Request(Method.PUT, Uri.of("/uri"))))
        assertThat(delete("/uri"), equalTo(Request(Method.DELETE, Uri.of("/uri"))))
        assertThat(options("/uri"), equalTo(Request(Method.OPTIONS, Uri.of("/uri"))))
        assertThat(trace("/uri"), equalTo(Request(Method.TRACE, Uri.of("/uri"))))
        assertThat(patch("/uri"), equalTo(Request(Method.PATCH, Uri.of("/uri"))))
    }

    private fun requestWithQuery(query: String) = Request(GET, Uri.of("http://ignore/?$query"))
}



