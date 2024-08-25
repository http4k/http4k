package org.http4k.azure

import com.azure.core.http.HttpHeader
import com.azure.core.http.HttpHeaderName
import com.azure.core.http.HttpHeaders
import com.azure.core.http.HttpMethod
import com.azure.core.http.HttpRequest
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.net.URI
import java.nio.ByteBuffer

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AzureHttpClientTest {

    @Test
    fun `converts formats correctly`() {
        val headers = listOf("foo" to "bar2", "foo" to "bar1")
        val request = Request(POST, "https://foobar/123")
            .query("foo", "bar1")
            .query("foo", "bar2")
            .headers(headers)
            .body("hello")

        val response = Response(I_M_A_TEAPOT).headers(headers).body("world")

        val client = AzureHttpClient {
            assertThat(it.toString(), equalTo(request.toString()))
            response
        }

        val out = client.send(
            HttpRequest(
                HttpMethod.POST,
                URI.create(request.uri.toString()).toURL(),
                HttpHeaders(headers.groupBy { it.first }.map { HttpHeader(it.key, it.value.map { it.second }) }),
                Flux.just(ByteBuffer.wrap("hello".toByteArray()))
            )
        ).block()
        assertThat(out.statusCode, equalTo(I_M_A_TEAPOT.code))
        assertThat(out.headers.getValues(HttpHeaderName.fromString("foo")).toList(), equalTo(listOf("bar2", "bar1")))
        assertThat(out.body.toInputStream().reader().readText(), equalTo(response.bodyString()))
    }
}
