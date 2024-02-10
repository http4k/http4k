package org.http4k.aws

import org.http4k.core.Request
import org.http4k.filter.Payload
import java.time.Clock
import java.time.Duration

fun interface AwsPreRequestSigner : (Request, Duration) -> AwsPreSignedRequest

fun AwsRequestPreSigner(scope: AwsCredentialScope, credentials: AwsCredentials, clock: Clock = Clock.systemUTC()) =
    AwsRequestPreSigner(
        scope = scope,
        credentialsProvider = { credentials },
        clock = clock
    )

fun AwsRequestPreSigner(
    scope: AwsCredentialScope,
    credentialsProvider: () -> AwsCredentials,
    clock: Clock = Clock.systemUTC()
): AwsPreRequestSigner = AwsPreRequestSigner { request, expires ->
    val awsDate = AwsRequestDate.of(clock.instant())
    val credentials = credentialsProvider()

    val fullRequest = request
        .replaceHeader("Host", "${request.uri.host}${request.uri.port?.let { port -> ":$port" } ?: ""}")
        .let { it.query("X-Amz-SignedHeaders", it.signedHeaders()) }
        .query("X-Amz-Algorithm", "AWS4-HMAC-SHA256")
        .query("X-Amz-Date", awsDate.full)
        .query("X-Amz-Credential", "${credentials.accessKey}/${scope.datedScope(awsDate)}")
        .query("X-Amz-Expires", expires.seconds.toString())
        .let {
            if (credentials.sessionToken != null) it.query(
                "X-Amz-Security-Token",
                credentials.sessionToken
            ) else it
        }

    val canonicalRequest =
        AwsCanonicalRequest.of(fullRequest, Payload.Mode.Unsigned(request))
    val signature = AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, awsDate)

    AwsPreSignedRequest(
        method = fullRequest.method,
        uri = fullRequest.query("X-Amz-Signature", signature).uri,
        signedHeaders = fullRequest.headers,
        expires = clock.instant() + expires
    )
}
