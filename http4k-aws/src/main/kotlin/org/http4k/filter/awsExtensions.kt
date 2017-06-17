package org.http4k.filter

import org.http4k.aws.AwsCanonicalRequest
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.AwsRequestDate
import org.http4k.aws.AwsSignatureV4Signer
import org.http4k.core.Filter
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
        "AWS4-HMAC-SHA256",
        credentials.accessKey, scope.datedScope(date),
        canonicalRequest.signedHeaders,
        AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date))
