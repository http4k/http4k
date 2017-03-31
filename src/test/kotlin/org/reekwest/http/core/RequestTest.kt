package org.reekwest.http.core

import org.reekwest.http.core.Method.GET
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class RequestTest {

    @Test
    fun extracts_query_parameters() {
        assertThat(requestWithQuery("foo=one&foo=two").queries("foo"), equalTo(listOf<String?>("one", "two")))
        assertThat(requestWithQuery("foo=one&foo=two").query("foo")!!, equalTo("one"))
        assertThat(requestWithQuery("foo=one&foo&foo=two").queries("foo"), equalTo(listOf("one", null, "two")))
    }

    private fun requestWithQuery(query: String) = Request(GET, Uri.uri("http://ignore/?$query"))
}



