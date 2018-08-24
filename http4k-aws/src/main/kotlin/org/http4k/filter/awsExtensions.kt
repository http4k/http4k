package org.http4k.filter

import org.http4k.aws.AwsCanonicalRequest
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsHmacSha256
import org.http4k.aws.AwsRequestDate
import org.http4k.aws.AwsSignatureV4Signer
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import java.time.Clock

fun ClientFilters.AwsAuth(scope: AwsCredentialScope,
                          credentials: AwsCredentials,
                          clock: Clock = Clock.systemDefaultZone(),
                          payloadMode: Payload.Mode = Payload.Mode.Signed) =
        Filter { next ->
            {
                val payload = payloadMode(it)

                val date = AwsRequestDate.of(clock.instant())

                val fullRequest = it
                        .replaceHeader("host", it.uri.host)
                        .replaceHeader("x-amz-date", date.full).let {
                            if (it.method.allowsContent) {
                                it.replaceHeader("content-length", payload.length.toString())
                            } else {
                                it
                            }
                        }


                val canonicalRequest = AwsCanonicalRequest.of(fullRequest, payload)

                val signedRequest = fullRequest
                        .replaceHeader("Authorization", buildAuthHeader(scope, credentials, canonicalRequest, date))
                        .replaceHeader("x-amz-content-sha256", payload.hash)

                next(signedRequest)
            }
        }

private val Method.allowsContent: Boolean
    get() = when (this) {
        HEAD -> false
        GET -> false
        OPTIONS -> false
        TRACE -> false
        DELETE -> false
        else -> true
    }

private fun buildAuthHeader(scope: AwsCredentialScope,
                            credentials: AwsCredentials,
                            canonicalRequest: AwsCanonicalRequest, date: AwsRequestDate) =
        String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
                "AWS4-HMAC-SHA256",
                credentials.accessKey, scope.datedScope(date),
                canonicalRequest.signedHeaders,
                AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date))

data class CanonicalPayload(val hash: String, val length: Long)

object Payload {
    sealed class Mode : (Request) -> CanonicalPayload {
        object Signed : Mode() {
            override operator fun invoke(request: Request) =
                    request.body.payload.array().let {
                        CanonicalPayload(AwsHmacSha256.hash(it), it.size.toLong())
                    }
        }

        object Unsigned : Mode() {
            override operator fun invoke(request: Request) = CanonicalPayload("UNSIGNED-PAYLOAD", request.body.length)
        }
    }
}