package org.http4k.gcp

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.InputStreamContent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class GcpSdkHttpTransportTest {

    @Test
    fun `converts formats correctly`() {
        val headers = listOf("foo" to "bar2")
        val request = Request(POST, "https://foobar/123")
            .query("foo", "bar1")
            .query("foo", "bar2")
            .headers(headers)
            .body("hello")

        val response = Response(OK).headers(headers).body("world")

        val client = GcpSdkHttpTransport {
            assertThat(it.method, equalTo(POST))
            assertThat(it.uri.toString(), equalTo("https://foobar/123?foo=bar1&foo=bar2"))
            assertThat(it.header("foo"), equalTo("bar2"))
            assertThat(it.bodyString(), equalTo("hello"))
            response
        }

        val out = client.createRequestFactory()
            .buildPostRequest(
                GenericUrl(request.uri.toString()),
                InputStreamContent("text/plain", "hello".byteInputStream())
            )
            .setHeaders(HttpHeaders().apply { headers.forEach { set(it.first, it.second) } })
            .execute()

        assertThat(out.statusCode, equalTo(OK.code))
        assertThat(out.headers["foo"], equalTo(listOf("bar2")))
        assertThat(out.content.reader().readText(), equalTo(response.bodyString()))
    }
}
