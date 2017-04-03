package org.reekwest.http.apache

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.entity.StringEntity
import org.reekwest.http.core.entity.extract
import org.reekwest.http.core.get
import org.reekwest.http.core.query

class ApacheHttpClientTest {
    val client = ApacheHttpClient()

    @Test
    fun basic_request() {
        val request = get("http://httpbin.org/get").query("name", "John Doe")
        val response = client(request)
        assertThat(response.status, equalTo(OK))
        assertThat(response.extract(StringEntity), containsSubstring("John Doe"))
    }
}

