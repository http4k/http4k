package org.http4k.aws

import org.http4k.security.HmacSha256
import org.http4k.security.HmacSha256.hmacSHA256
import org.http4k.util.Hex

internal object AwsSignatureV4Signer {

    fun sign(
        request: AwsCanonicalRequest,
        scope: AwsCredentialScope,
        awsCredentials: AwsCredentials,
        date: AwsRequestDate
    ): String {
        val signatureKey = getSignatureKey(awsCredentials.secretKey, date.basic, scope.region, scope.service)
        val signature = hmacSHA256(signatureKey, request.stringToSign(scope, date))
        return Hex.hex(signature)
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(charset("UTF8"))
        val kDate = hmacSHA256(kSecret, dateStamp)
        val kRegion = hmacSHA256(kDate, regionName)
        val kService = hmacSHA256(kRegion, serviceName)
        return hmacSHA256(kService, "aws4_request")
    }

    private fun AwsCanonicalRequest.stringToSign(requestScope: AwsCredentialScope, date: AwsRequestDate) =
        "AWS4-HMAC-SHA256" +
            "\n" +
            date.full +
            "\n" +
            requestScope.datedScope(date) +
            "\n" +
            HmacSha256.hash(value)
}
