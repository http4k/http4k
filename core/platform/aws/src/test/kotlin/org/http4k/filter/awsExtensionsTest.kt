package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.isEmptyString
import org.http4k.asByteBuffer
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.ByteBuffer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FilterExtensionsTest {
    @Test
    fun `set base aws service url`() {
        val app = ClientFilters.SetAwsServiceUrl("myservice", "narnia")
            .then { Response(Status.OK).body(it.uri.toString()) }
        assertThat(app(Request(Method.GET, "/bob")), hasBody("https://myservice.narnia.amazonaws.com/bob"))
    }

    private val awsCredentialScope = AwsCredentialScope("narnia", "myservice")
    private val awsCredentials = AwsCredentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
    private val clock = Clock.fixed(Instant.parse("2024-02-07T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `authorize aws request with no body content`() {
        // given
        lateinit var capturedRequest: Request
        val app = ClientFilters.AwsAuth(
            scope = awsCredentialScope,
            credentials = awsCredentials,
            clock = clock
        ).then { request ->
            capturedRequest = request
            Response(Status.OK)
        }

        // when
        app(Request(Method.GET, "http://localhost:8000/foo"))

        // then
        assertThat(capturedRequest, hasHeader("host", "localhost:8000"))
        assertThat(capturedRequest, hasHeader("x-amz-content-sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
        assertThat(capturedRequest, hasHeader("x-amz-date", "20240207T000000Z"))
        assertThat(capturedRequest, hasHeader("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240207/narnia/myservice/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=314a5fa82a1688d2f1ac0ee8787276306235d5f9870723274d828d66401590c7"))
    }

    @Test
    fun `authorize aws request with signed body content`() {
        // given
        lateinit var capturedRequest: Request
        val app = ClientFilters.AwsAuth(
            scope = awsCredentialScope,
            credentials = awsCredentials,
            clock = clock
        ).then { request ->
            capturedRequest = request
            Response(Status.OK)
        }

        val content = "this is some content to upload"

        // when
        app(Request(Method.PUT, "http://localhost:8000/foo").body(content))

        // then
        assertThat(capturedRequest, hasHeader("host", "localhost:8000"))
        assertThat(capturedRequest, hasHeader("x-amz-content-sha256", "0b2cede50bd744966a5571382f321d17fa174a19a1120e9823fed2ca484d6250"))
        assertThat(capturedRequest, hasHeader("x-amz-date", "20240207T000000Z"))
        assertThat(capturedRequest, hasHeader("content-length", content.length.toString()))
        assertThat(capturedRequest, hasHeader("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240207/narnia/myservice/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=30520c468aca1034456bafa36f10cdc95d0ef6366569d69d7da30c8e2c6c2ff9"))
    }

    @Test
    fun `authorize aws request with unsigned body content`() {
        // given
        lateinit var capturedRequest: Request
        val app = ClientFilters.AwsAuth(
            scope = awsCredentialScope,
            credentials = awsCredentials,
            clock = clock,
            payloadMode = Payload.Mode.Unsigned
        ).then { request ->
            capturedRequest = request
            Response(Status.OK)
        }

        val content = "this is some content to upload"

        // when
        app(Request(Method.PUT, "http://localhost:8000/foo").body(content))

        // then
        assertThat(capturedRequest, hasHeader("host", "localhost:8000"))
        assertThat(capturedRequest, hasHeader("x-amz-content-sha256", "UNSIGNED-PAYLOAD"))
        assertThat(capturedRequest, hasHeader("x-amz-date", "20240207T000000Z"))
        assertThat(capturedRequest, hasHeader("content-length", content.length.toString()))
        assertThat(capturedRequest, hasHeader("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240207/narnia/myservice/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=c760a7a420fceab051da0f2094896df09d802d740ad388dc0acb81505771929d"))
    }
}
