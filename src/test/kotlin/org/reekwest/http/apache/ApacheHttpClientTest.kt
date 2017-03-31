package org.reekwest.http.apache

import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ApacheHttpClientTest {
    val client = ApacheHttpClient()

    @Test
    fun basic_request() {
        val response = client(Request(GET, Uri.uri("http://httpbin.org/get")))
        assertThat(response.status, equalTo(OK))
        assertThat(response.entity.toString(), containsSubstring("}"))
    }
}

