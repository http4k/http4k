package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Test
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

    @Test
    fun `authorize aws request`() {
        // given
        lateinit var capturedRequest: Request
        val app = ClientFilters.AwsAuth(
            scope = AwsCredentialScope("narnia", "myservice"),
            credentials = AwsCredentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"),
            clock = Clock.fixed(Instant.parse("2024-02-07T00:00:00Z"), ZoneOffset.UTC)
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
}
