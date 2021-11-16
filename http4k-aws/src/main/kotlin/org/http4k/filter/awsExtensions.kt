package org.http4k.filter

import org.http4k.aws.AwsCanonicalRequest
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsRequestDate
import org.http4k.aws.AwsSignatureV4Signer
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.HmacSha256.hash
import java.time.Clock

/**
 * Sign AWS requests using static credentials.
 */
fun ClientFilters.AwsAuth(scope: AwsCredentialScope,
                          credentials: AwsCredentials,
                          clock: Clock = Clock.systemDefaultZone(),
                          payloadMode: Payload.Mode = Payload.Mode.Signed) = ClientFilters.AwsAuth(scope, { credentials }, clock, payloadMode)

/**
 * Sign AWS requests using dynamically provided (expiring) credentials.
 */
fun ClientFilters.AwsAuth(scope: AwsCredentialScope,
                          credentialsProvider: () -> AwsCredentials,
                          clock: Clock = Clock.systemDefaultZone(),
                          payloadMode: Payload.Mode = Payload.Mode.Signed) =
    Filter { next ->
        {
            val payload = payloadMode(it)

            val credentials = credentialsProvider()

            val date = AwsRequestDate.of(clock.instant())

            val fullRequest = it
                .replaceHeader("host", it.uri.host)
                .replaceHeader("x-amz-content-sha256", payload.hash)
                .replaceHeader("x-amz-date", date.full).let {
                    if (it.method.allowsContent) {
                        it.replaceHeader("content-length", payload.length.toString())
                    } else {
                        it
                    }
                }.run {
                    credentials.sessionToken?.let {
                        replaceHeader("x-amz-security-token", credentials.sessionToken)
                    } ?: this
                }

            val canonicalRequest = AwsCanonicalRequest.of(fullRequest, payload)

            val signedRequest = fullRequest
                .replaceHeader("Authorization", buildAuthHeader(scope, credentials, canonicalRequest, date))

            next(signedRequest.body(Body(it.body.payload)))
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
    "AWS4-HMAC-SHA256 Credential=${credentials.accessKey}/${scope.datedScope(date)}, SignedHeaders=${canonicalRequest.signedHeaders}, Signature=${AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date)}"

data class CanonicalPayload(val hash: String, val length: Long)

object Payload {
    sealed class Mode : (Request) -> CanonicalPayload {
        object Signed : Mode() {
            override operator fun invoke(request: Request) =
                request.body.payload.array().let {
                    CanonicalPayload(hash(it), it.size.toLong())
                }
        }

        object Unsigned : Mode() {
            override operator fun invoke(request: Request) = CanonicalPayload(
                "UNSIGNED-PAYLOAD",
                request.body.length ?: throw IllegalStateException("request body size could not be determined"))
        }
    }
}

fun ClientFilters.SetAwsServiceUrl(serviceName: String, region: String) =
    SetHostFrom(Uri.of("https://$serviceName.${region}.amazonaws.com"))
