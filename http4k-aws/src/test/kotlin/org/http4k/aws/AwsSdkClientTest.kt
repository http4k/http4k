package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import software.amazon.awssdk.http.HttpExecuteRequest
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpFullResponse
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.http.SdkHttpResponse
import java.net.URL

class AwsSdkClientTest {
    @Test
    fun `converts formats correctly`() {
        val headers = listOf("foo" to "bar1", "bar" to null)
        val expected = Request(POST, "https://foobar/123")
            .query("foo", "bar1")
            .query("foo", "bar2")
            .headers(headers)
            .body("hello")

        val client = AwsSdkClient {
            assertThat(it.toString(), equalTo(expected.toString()))
            Response(OK).headers(headers).body("world")
        }

        val out = client.prepareRequest(
            HttpExecuteRequest.builder().request(
                SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.POST)
                    .uri(URL(expected.uri.toString()).toURI())
                    .contentStreamProvider { expected.body.stream }.build()
            ).build()
        ).call()

        assertThat(out.httpResponse(), equalTo<SdkHttpResponse>(SdkHttpFullResponse.builder().build()))
    }
}
