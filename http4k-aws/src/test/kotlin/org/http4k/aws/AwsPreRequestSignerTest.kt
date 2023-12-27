package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.hamkrest.hasHost
import org.http4k.hamkrest.hasQuery
import org.http4k.hamkrest.hasUriPath
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AwsPreRequestSignerTest {

    @Test
    fun `signed with standard credentials`() {
        val time = Instant.parse("2013-05-24T00:00:00Z")
        val signer = AwsPreRequestSigner(
            scope = AwsCredentialScope("us-east-1", "s3"),
            credentialsProvider = { AwsCredentials("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY") },
            clock = Clock.fixed(time, ZoneOffset.UTC)
        )
        val request = Request(Method.GET, "https://examplebucket.s3.amazonaws.com/test.txt")
        val signed = signer(request, Duration.ofHours(24))
        val signedRequest = request.uri(signed.uri)

        assertThat(signed.uri, hasHost("examplebucket.s3.amazonaws.com"))
        assertThat(signed.uri, hasUriPath("/test.txt"))
        assertThat(signedRequest, hasQuery("X-Amz-Algorithm", equalTo("AWS4-HMAC-SHA256")))
        assertThat(signedRequest, hasQuery("X-Amz-Credential", equalTo("AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request")))
        assertThat(signedRequest, hasQuery("X-Amz-Date", equalTo("20130524T000000Z")))
        assertThat(signedRequest, hasQuery("X-Amz-Expires", equalTo("86400")))
        assertThat(signedRequest, hasQuery("X-Amz-SignedHeaders", equalTo("host")))
        assertThat(signedRequest, hasQuery("X-Amz-Signature", equalTo("aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404")))

        assertThat(signed.signedHeaders, equalTo(listOf(
            "Host" to "examplebucket.s3.amazonaws.com"
        )))
        assertThat(signed.expires, equalTo(time + Duration.ofHours(24)))
    }

    @Test
    fun `signed with STS credentials`() {
        val signer = AwsPreRequestSigner(
            scope = AwsCredentialScope("us-east-1", "s3"),
            credentials = AwsCredentials(
                accessKey = "AKIAIOSFODNN7EXAMPLE",
                secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                sessionToken = "SESSION_TOKEN"
            ),
            clock = Clock.fixed(Instant.parse("2013-05-24T00:00:00Z"), ZoneOffset.UTC)
        )
        val request = Request(Method.GET, "https://examplebucket.s3.amazonaws.com/test.txt")
        val signed = request
            .uri(signer(request, Duration.ofHours(24)).uri)

        assertThat(signed.uri, hasHost("examplebucket.s3.amazonaws.com"))
        assertThat(signed.uri, hasUriPath("/test.txt"))
        assertThat(signed, hasQuery("X-Amz-Algorithm", equalTo("AWS4-HMAC-SHA256")))
        assertThat(signed, hasQuery("X-Amz-Credential", equalTo("AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request")))
        assertThat(signed, hasQuery("X-Amz-Date", equalTo("20130524T000000Z")))
        assertThat(signed, hasQuery("X-Amz-Expires", equalTo("86400")))
        assertThat(signed, hasQuery("X-Amz-SignedHeaders", equalTo("host")))
        assertThat(signed, hasQuery("X-Amz-Security-Token", equalTo("SESSION_TOKEN")))
        assertThat(signed, hasQuery("X-Amz-Signature", equalTo("2d6c965684dab7f9323f29d2dcbb33660cc71630460c0d5a439c0719a708f274")))
    }
}
