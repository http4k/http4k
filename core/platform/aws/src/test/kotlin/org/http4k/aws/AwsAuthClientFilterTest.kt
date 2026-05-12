package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.time.Clock.fixed
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AwsClientFilterTest: PortBasedTest {

    private val scope = AwsCredentialScope("us-east", "s3")
    private val credentials = AwsCredentials("access", "secret")
    private val iamSessionCredentials = AwsCredentials("access", "secret", "sessionToken")

    private val clock = fixed(LocalDateTime.of(2016, 1, 27, 15, 32, 50, 27).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

    private val audit = AuditHandler()

    private val client = ClientFilters.AwsAuth(scope, credentials, clock).then(audit)

    @Test
    fun `adds authorization header`() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(
            audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/us-east/s3/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=80641a5d87dd9aad7a993b341c98a04cdefa58ecf09f1df4af93cec0268a8eca")
        )
    }

    @Test
    fun `adds authorization header with session token`() {
        val client = ClientFilters.AwsAuth(scope, iamSessionCredentials, clock).then(audit)

        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(
            audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/us-east/s3/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date;x-amz-security-token, Signature=f0522e59ba6c8d851970a1d4fb510cc37bb18330b66958992f653fc8966a2137")
        )
    }

    @Test
    fun `adds time header`() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(audit.captured?.header("x-amz-date"), equalTo("20160127T153250Z"))
    }

    @Test
    fun `stream body is send correctly after signing`() {
        val client = ClientFilters.AwsAuth(scope, credentials, clock)
            .then { Response(OK).body(it.body.stream) }

        val response = client(Request(GET, "http://amazon/test").body("foobar".byteInputStream()))

        assertThat(response.bodyString(), equalTo("foobar"))
    }

    @Test
    fun `stream body is send correctly after signing - over socket`() {
        val http = { req: Request -> Response(OK).body(req.bodyString()) }

        http.asServer(SunHttp(0)).start().use {
            val client = ClientFilters.AwsAuth(scope, credentials, clock)
                .then(JavaHttpClient())
            val response = client(Request(GET, "http://localhost:${it.port()}").body("foobar".byteInputStream()))

            assertThat(response.bodyString(), equalTo("foobar"))
        }
    }

    @Test
    fun adds_content_sha256() {
        client(Request(GET, "http://amazon/test").header("host", "foobar").header("content-length", "0"))

        assertThat(
            audit.captured?.header("x-amz-content-sha256"),
            equalTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        )
    }


    @Test
    fun `path canonicalization for lambda service`() {

        val scope = AwsCredentialScope("eu-west", "lambda")

        val client = ClientFilters.AwsAuth(scope, credentials, clock).then(audit)

        client(Request(POST, Uri.of("https://fgjrg66cimblkqcbir4yznv5wm0dkexm.lambda-url.eu-west-1.on.aws/tasks:xxxx")))

        assertThat(
            audit.captured?.uri,
            equalTo(Uri.of("https://fgjrg66cimblkqcbir4yznv5wm0dkexm.lambda-url.eu-west-1.on.aws/tasks%3Axxxx"))
        )
        assertThat(
            audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/eu-west/lambda/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=6441e79e477d75a2bd4e064e961550f4aebb1dfce9aec6f28e9030610eed1c4c")
        )
    }

    @Test
    fun `path canonicalization for s3 service`() {

        val scope = AwsCredentialScope("eu-west", "s3")

        val client = ClientFilters.AwsAuth(scope, credentials, clock).then(audit)

        client(Request(POST, Uri.of("https://fgjrg66cimblkqcbir4yznv5wm0dkexm.lambda-url.eu-west-1.on.aws/tasks:xxxx")))

        assertThat(
            audit.captured?.uri,
            equalTo(Uri.of("https://fgjrg66cimblkqcbir4yznv5wm0dkexm.lambda-url.eu-west-1.on.aws/tasks%3Axxxx"))
        )
        assertThat(
            audit.captured?.header("Authorization"),
            equalTo("AWS4-HMAC-SHA256 Credential=access/20160127/eu-west/s3/aws4_request, SignedHeaders=content-length;host;x-amz-content-sha256;x-amz-date, Signature=15b575a0b48e6b9a19a33964f87e169556d835e879c77dd0f6f807ea834980c9")
        )
    }
}

class AuditHandler : HttpHandler {
    var captured: Request? = null

    override fun invoke(request: Request): Response {
        captured = request
        return Response(OK)
    }
}
