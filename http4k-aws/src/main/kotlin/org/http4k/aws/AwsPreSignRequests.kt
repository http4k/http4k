package org.http4k.aws

import org.http4k.core.Request
import org.http4k.filter.Payload
import java.time.Clock
import java.time.Duration
import java.util.Locale

class AwsPreSignRequests(
    private val scope: AwsCredentialScope,
    private val credentials: AwsCredentials,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(request: Request, expires: Duration): AwsPreSignedRequest {
        val time = clock.instant()
        val awsDate = AwsRequestDate.of(time)

        val headers = request.headers + ("host" to request.uri.host)
        val signedHeaders = headers.map { it.first.lowercase(Locale.getDefault()) }.toSet().sorted().joinToString(";")

        val fullRequest = request
            .replaceHeader("Host", request.uri.host)
            .query("X-Amz-Algorithm", "AWS4-HMAC-SHA256")
            .query("X-Amz-Date", awsDate.full)
            .query("X-Amz-SignedHeaders", signedHeaders)
            .query("X-Amz-Credential", "${credentials.accessKey}/${scope.datedScope(awsDate)}")
            .query("X-Amz-Expires", expires.seconds.toString())

        val canonicalRequest = AwsCanonicalRequest.of(fullRequest, Payload.Mode.Unsigned(request))
        val signature = AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, awsDate)

        return AwsPreSignedRequest(
            uri = fullRequest.query("X-Amz-Signature", signature).uri,
            signedHeaders = fullRequest.headers,
            expires = time + expires
        )
    }
}
