package org.http4k.aws

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.toParameters
import org.http4k.urlEncoded
import java.io.UnsupportedEncodingException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AwsCredentials(val accessKey: String, val secretKey: String)

data class AwsCredentialScope(val region: String, val service: String)

class AwsHttpClient(private val clock: Clock,
                    private val scope: AwsCredentialScope,
                    private val credentials: AwsCredentials) : Filter {
    companion object {
        val ALGORITHM = "AWS4-HMAC-SHA256"
    }

    override fun invoke(next: HttpHandler): HttpHandler = {
        request ->
        val date = AwsRequestDate.of(clock.instant())

        val fullRequest = request
            .header("host", request.uri.host)
            .header("x-amz-date", date.full)

        val canonicalRequest = AwsCanonicalRequest.of(fullRequest)

        val signedRequest = fullRequest
            .header("Authorization", buildAuthHeader(canonicalRequest, date))
            .header("x-amz-content-sha256", canonicalRequest.payloadHash)

        next(signedRequest)
    }

    private fun buildAuthHeader(canonicalRequest: AwsCanonicalRequest, date: AwsRequestDate) =
        String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
            ALGORITHM,
            credentials.accessKey, scope.datedScope(date),
            canonicalRequest.signedHeaders,
            AwsSignatureV4Signer.sign(canonicalRequest, scope, credentials, date))
}

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

        private fun Request.payloadHash(): String =
            (body?.payload?.array() ?: "".toByteArray()).let { AwsHmacSha256.hash(it) }
    }
}

internal data class AwsRequestDate(val basic: String, val full: String) {
    companion object {
        private val FORMAT_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"))
        private val FORMAT_FULL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"))

        fun of(instant: Instant): AwsRequestDate =
            AwsRequestDate(FORMAT_BASIC.format(instant), FORMAT_FULL.format(instant))
    }
}

internal object AwsSignatureV4Signer {

    fun sign(request: AwsCanonicalRequest, scope: AwsCredentialScope, awsCredentials: AwsCredentials, date: AwsRequestDate): String {
        val signatureKey = getSignatureKey(awsCredentials.secretKey, date.basic, scope.region, scope.service)
        val signature = AwsHmacSha256.hmacSHA256(signatureKey, request.stringToSign(scope, date))
        return AwsHmacSha256.hex(signature)
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        try {
            val kSecret = ("AWS4" + key).toByteArray(charset("UTF8"))
            val kDate = AwsHmacSha256.hmacSHA256(kSecret, dateStamp)
            val kRegion = AwsHmacSha256.hmacSHA256(kDate, regionName)
            val kService = AwsHmacSha256.hmacSHA256(kRegion, serviceName)
            return AwsHmacSha256.hmacSHA256(kService, "aws4_request")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException("Could not generate signature key", e)
        }
    }

    private fun AwsCanonicalRequest.stringToSign(requestScope: AwsCredentialScope, date: AwsRequestDate) =
        AwsHttpClient.ALGORITHM +
            "\n" +
            date.full +
            "\n" +
            requestScope.datedScope(date) +
            "\n" +
            AwsHmacSha256.hash(value)
}

internal fun AwsCredentialScope.datedScope(date: AwsRequestDate): String =
    String.format("%s/%s/%s/aws4_request",
        date.basic,
        region,
        service)