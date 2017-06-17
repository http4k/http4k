package org.http4k.filter

import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsHmacSha256
import org.http4k.aws.AwsRequestDate
import org.http4k.aws.AwsSignatureV4Signer
import org.http4k.aws.datedScope
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.toParameters
import org.http4k.urlEncoded
import sun.security.x509.CertificateAlgorithmId.ALGORITHM
import java.time.Clock

fun ClientFilters.AwsAuth(scope: AwsCredentialScope,
                          credentials: AwsCredentials,
                          clock: Clock = Clock.systemDefaultZone()) =
    Filter {
        next ->
        {
            val date = AwsRequestDate.of(clock.instant())

            val fullRequest = it
                .header("host", it.uri.host)
                .header("x-amz-date", date.full)

            val canonicalRequest = AwsCanonicalRequest.of(fullRequest)

            val signedRequest = fullRequest
                .header("Authorization", buildAuthHeader(scope, credentials, canonicalRequest, date))
                .header("x-amz-content-sha256", canonicalRequest.payloadHash)

            next(signedRequest)
        }
    }

private fun buildAuthHeader(scope: AwsCredentialScope,
                            credentials: AwsCredentials,
                            canonicalRequest: AwsCanonicalRequest, date: AwsRequestDate) =
    String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
        ALGORITHM,
        credentials.accessKey, scope.datedScope(date),
        canonicalRequest.signedHeaders,
        AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date))

internal data class AwsCanonicalRequest(val value: String, val signedHeaders: String, val payloadHash: String) {
    companion object {
        fun of(request: Request): AwsCanonicalRequest {
            val signedHeaders = request.signedHeaders()
            val payloadHash = request.payloadHash()
            val canonical = request.method.name +
                "\n" +
                request.uri.path +
                "\n" +
                request.canonicalQueryString() +
                "\n" +
                request.canonicalHeaders() +
                "\n\n" +
                signedHeaders +
                "\n" +
                payloadHash
            return AwsCanonicalRequest(canonical, signedHeaders, payloadHash)
        }

        private fun Request.signedHeaders(): String =
            headers.map { it.first.toLowerCase() }.sorted().joinToString(";")

        private fun Request.canonicalHeaders(): String = headers
            .map { it.first.toLowerCase() to it.second?.replace("\\s+", " ")?.trim() }
            .map { it.first + ":" + it.second }
            .sorted()
            .joinToString("\n")

        private fun Request.canonicalQueryString(): String =
            uri.query.toParameters()
                .map { (first, second) -> first.urlEncoded() + "=" + second?.urlEncoded() }
                .sorted()
                .joinToString("&")

        private fun Request.payloadHash(): String = AwsHmacSha256.hash(body.payload.array())
    }
}
