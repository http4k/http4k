package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request.Companion.delete
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.options
import org.reekwest.http.core.Request.Companion.patch
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.Request.Companion.put
import org.reekwest.http.core.Request.Companion.trace

class RequestTest {

    @Test
    fun extracts_query_parameters() {
        assertThat(requestWithQuery("foo=one&foo=two").queries("foo"), equalTo(listOf<String?>("one", "two")))
        assertThat(requestWithQuery("foo=one&foo=two").query("foo")!!, equalTo("one"))
        assertThat(requestWithQuery("foo=one&foo&foo=two").queries("foo"), equalTo(listOf("one", null, "two")))
    }

    @Test
    fun can_create_using_method_and_uri_string() {
        assertThat(get("/uri"), equalTo(Request(GET, Uri.uri("/uri"))))
        assertThat(post("/uri"), equalTo(Request(Method.POST, Uri.uri("/uri"))))
        assertThat(put("/uri"), equalTo(Request(Method.PUT, Uri.uri("/uri"))))
        assertThat(delete("/uri"), equalTo(Request(Method.DELETE, Uri.uri("/uri"))))
        assertThat(options("/uri"), equalTo(Request(Method.OPTIONS, Uri.uri("/uri"))))
        assertThat(trace("/uri"), equalTo(Request(Method.TRACE, Uri.uri("/uri"))))
        assertThat(patch("/uri"), equalTo(Request(Method.PATCH, Uri.uri("/uri"))))
    }

    private fun requestWithQuery(query: String) = Request(GET, Uri.uri("http://ignore/?$query"))
}



