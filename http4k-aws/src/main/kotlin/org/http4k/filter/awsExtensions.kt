package org.http4k.filter

import org.http4k.aws.AwsCanonicalRequest
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsHmacSha256
import org.http4k.aws.AwsRequestDate
import org.http4k.aws.AwsSignatureV4Signer
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Request
import java.time.Clock

fun ClientFilters.AwsAuth(scope: AwsCredentialScope,
                          credentials: AwsCredentials,
                          clock: Clock = Clock.systemDefaultZone(),
                          payloadMode: Payload.Mode = Payload.Mode.Signed) =
    Filter {
        next ->
        {
            val payload = payloadMode(it)

            val date = AwsRequestDate.of(clock.instant())

            val fullRequest = it
                .header("host", it.uri.host)
                .header("x-amz-date", date.full)
                .replaceHeader("content-length", payload.length.toString())

            val canonicalRequest = AwsCanonicalRequest.of(fullRequest, payload)

            val signedRequest = fullRequest
                .header("Authorization", buildAuthHeader(scope, credentials, canonicalRequest, date))
                .header("x-amz-content-sha256", payload.hash)

            next(signedRequest)
        }
    }

private fun buildAuthHeader(scope: AwsCredentialScope,
                             credentials: AwsCredentials,
                             canonicalRequest: AwsCanonicalRequest, date: AwsRequestDate) =
    String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
        "AWS4-HMAC-SHA256",
        credentials.accessKey, scope.datedScope(date),
        canonicalRequest.signedHeaders,
        AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date))

data class CanonicalPayload(val hash: String, val length: Int){
    companion object {
        val EMPTY = from(Body.EMPTY)
        fun from(body: Body) = CanonicalPayload( AwsHmacSha256.hash(body.payload.array()), body.payload.array().size)
    }
}

object Payload {
    sealed class Mode : (Request) -> CanonicalPayload {
        object Signed : Mode() {
            override operator fun invoke(request: Request) = CanonicalPayload.from(request.body)
        }

        object Unsigned : Mode() {
            override operator fun invoke(request: Request) = CanonicalPayload("UNSIGNED-PAYLOAD", request.header("content-length")?.toInt() ?: 0)
        }
    }
}