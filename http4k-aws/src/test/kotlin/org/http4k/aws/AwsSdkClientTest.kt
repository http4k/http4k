package org.http4k.aws

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.junit.jupiter.api.Test
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.http.HttpExecuteRequest.builder
import software.amazon.awssdk.http.HttpExecuteResponse
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpFullResponse
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.http.SdkHttpResponse
import java.net.URL
import java.util.Optional

class AwsSdkClientTest {

    @Test
    fun `converts formats correctly`() {
        val headers = listOf("bar" to null, "foo" to "bar1")
        val request = Request(POST, "https://foobar/123")
            .query("foo", "bar1")
            .query("foo", "bar2")
            .headers(headers)
            .body("hello")

        val response = Response(I_M_A_TEAPOT).headers(headers).body("world")

        val client = AwsSdkClient {
            assertThat(it.toString(), equalTo(request.toString()))
            response
        }

        val out = client.prepareRequest(
            builder().request(
                SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.POST)
                    .headers(headers.groupBy { it.first }
                        .mapValues { it.value.map { it.second } })
                    .uri(URL(request.uri.toString()).toURI())
                    .putRawQueryParameter("foo", listOf("bar1", "bar2"))
                    .contentStreamProvider { request.body.stream }.build()
            ).build()
        ).call()

        assertThat(out.httpResponse() as SdkHttpFullResponse,
            has(SdkHttpResponse::statusCode, equalTo(I_M_A_TEAPOT.code))
                .and(
                    has(SdkHttpFullResponse::headers, equalTo(mapOf(
                        "bar" to listOf(null),
                        "foo" to listOf("bar1")
                    )))
                )
        )

        assertThat(
            out,
            has(HttpExecuteResponse::responseBody, object : Matcher<Optional<AbortableInputStream>> {
                override val description = "same content"

                override fun invoke(actual: Optional<AbortableInputStream>): MatchResult {
                    val content = actual.get().reader().readText()
                    return if (content == response.bodyString()) MatchResult.Match else MatchResult.Mismatch(content)
                }
            }
            )
        )
    }
}
