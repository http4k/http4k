package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Test
import java.time.Clock.fixed
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AwsClientFilterTest {

    private val scope = AwsCredentialScope("us-east", "s3")
    private val credentials = AwsCredentials("access", "secret")
    private val iamSessionCredentials = AwsCredentials("access", "secret", "sessionToken")

    private val clock = fixed(LocalDateTime.of(2016, 1, 27, 15, 32, 50, 27).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

    private val audit = AuditHandler()

    private val client = ClientFilters.AwsAuth(scope, credentials, clock).then(audit)

    @Test
    fun `adds authorization header`() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/us-east/s3/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=80641a5d87dd9aad7a993b341c98a04cdefa58ecf09f1df4af93cec0268a8eca"))
    }

    @Test
    fun `adds authorization header with session token`() {
        val client = ClientFilters.AwsAuth(scope, iamSessionCredentials, clock).then(audit)

        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/us-east/s3/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date;x-amz-security-token, Signature=f0522e59ba6c8d851970a1d4fb510cc37bb18330b66958992f653fc8966a2137"))
    }

    @Test
    fun `adds time header`() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(audit.captured?.header("x-amz-date"), equalTo("20160127T153250Z"))
    }

    @Test
    fun adds_content_sha256() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(audit.captured?.header("x-amz-content-sha256"),
            equalTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
    }
}

class AuditHandler : HttpHandler {
    var captured: Request? = null

    override fun invoke(request: Request): Response {
        captured = request
        return Response(OK)
    }
}
