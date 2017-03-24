package com.gourame.http.apache

import com.gourame.http.core.Method.GET
import com.gourame.http.core.Request
import com.gourame.http.core.Status.Companion.OK
import com.gourame.http.core.Uri
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

