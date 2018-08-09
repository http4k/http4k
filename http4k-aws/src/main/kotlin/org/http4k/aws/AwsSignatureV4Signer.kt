package org.http4k.aws

import java.io.UnsupportedEncodingException

internal object AwsSignatureV4Signer {

    fun sign(request: AwsCanonicalRequest, scope: AwsCredentialScope, awsCredentials: AwsCredentials, date: AwsRequestDate): String {
        val signatureKey = getSignatureKey(awsCredentials.secretKey, date.basic, scope.region, scope.service)
        val signature = AwsHmacSha256.hmacSHA256(signatureKey, request.stringToSign(scope, date))
        return AwsHmacSha256.hex(signature)
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray = try {
        val kSecret = ("AWS4$key").toByteArray(charset("UTF8"))
        val kDate = AwsHmacSha256.hmacSHA256(kSecret, dateStamp)
        val kRegion = AwsHmacSha256.hmacSHA256(kDate, regionName)
        val kService = AwsHmacSha256.hmacSHA256(kRegion, serviceName)
        AwsHmacSha256.hmacSHA256(kService, "aws4_request")
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException("Could not generate signature key", e)
    }

    private fun AwsCanonicalRequest.stringToSign(requestScope: AwsCredentialScope, date: AwsRequestDate) =
        "AWS4-HMAC-SHA256" +
            "\n" +
            date.full +
            "\n" +
            requestScope.datedScope(date) +
            "\n" +
            AwsHmacSha256.hash(value)
}