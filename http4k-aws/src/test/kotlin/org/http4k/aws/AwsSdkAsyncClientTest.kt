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
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpFullResponse
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.AsyncExecuteRequest.builder
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler
import software.amazon.awssdk.utils.BinaryUtils
import java.net.URI
import java.nio.ByteBuffer
import java.util.Optional

private fun testResponseHandler(responseBuilder: SdkHttpFullResponse.Builder) = object: SdkAsyncHttpResponseHandler {

    override fun onHeaders(headers: SdkHttpResponse) {
        responseBuilder.statusCode(headers.statusCode())
        responseBuilder.statusText(headers.statusText().orElse(null))
        responseBuilder.headers(headers.headers())
    }

    override fun onStream(stream: Publisher<ByteBuffer>) {
        stream.subscribe(object: Subscriber<ByteBuffer> {
            override fun onSubscribe(s: Subscription) {
                s.request(Long.MAX_VALUE)
            }

            override fun onError(t: Throwable) { throw t }
            override fun onComplete() { }

            override fun onNext(t: ByteBuffer) {
                responseBuilder.content(AbortableInputStream.create(BinaryUtils.toStream(t)))
            }
        })
    }

    override fun onError(error: Throwable) {}
}

class AwsSdkAsyncClientTest {

    @Test
    fun `converts formats correctly`() {
        val headers = listOf("bar" to null, "foo" to "bar1")
        val request = Request(POST, "https://foobar/123")
            .query("foo", "bar1")
            .query("foo", "bar2")
            .headers(headers)
            .body("hello")

        val response = Response(I_M_A_TEAPOT).headers(headers).body("world")

        val client = AwsSdkAsyncClient {
            assertThat(it.toString(), equalTo(request.toString()))
            response
        }

        val responseBuilder = SdkHttpFullResponse.builder()

        client.execute(
            builder().request(
                SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.POST)
                    .headers(headers.groupBy { it.first }
                        .mapValues { it.value.map { it.second } })
                    .uri(URI.create(request.uri.toString()))
                    .putRawQueryParameter("foo", listOf("bar1", "bar2"))
                    .contentStreamProvider { request.body.stream }.build()
            )
                .responseHandler(testResponseHandler(responseBuilder))
                .build()
        ).get()

        val out = responseBuilder.build()

        assertThat(out,
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
            has(SdkHttpFullResponse::content, object : Matcher<Optional<AbortableInputStream>> {
                override val description = "same content"

                override fun invoke(actual: Optional<AbortableInputStream>): MatchResult {
                    val content = actual.get().reader().readText()
                    return if (content == response.bodyString()) MatchResult.Match else MatchResult.Mismatch(content)
                }
            })
        )
    }
}
