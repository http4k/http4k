package com.gourame.http.apache

import com.gourame.http.apache.ApacheHttpClient
import com.gourame.http.core.Method.GET
import com.gourame.http.core.Request
import com.gourame.http.core.Status
import com.gourame.http.core.Uri
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ApacheHttpClientTest {
    val client = ApacheHttpClient()

    @Test
    fun basic_request() {
        val response = client(Request(GET, Uri("http://httpbin.org/get")))
        assertThat(response.status, equalTo(Status.Companion.OK))
        assertThat(response.entity.toString(), containsSubstring("}"))
    }
}

