package org.http4k.aws

import org.http4k.security.Sha256
import org.http4k.security.Sha256.hmac
import org.http4k.util.Hex

internal object AwsSignatureV4Signer {

    fun sign(
        request: AwsCanonicalRequest,
        scope: AwsCredentialScope,
        awsCredentials: AwsCredentials,
        date: AwsRequestDate
    ): String {
        val signatureKey = getSignatureKey(awsCredentials.secretKey, date.basic, scope.region, scope.service)
        val signature = hmac(signatureKey, request.stringToSign(scope, date))
        return Hex.hex(signature)
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(charset("UTF8"))
        val kDate = hmac(kSecret, dateStamp)
        val kRegion = hmac(kDate, regionName)
        val kService = hmac(kRegion, serviceName)
        return hmac(kService, "aws4_request")
    }

    private fun AwsCanonicalRequest.stringToSign(requestScope: AwsCredentialScope, date: AwsRequestDate) =
        "AWS4-HMAC-SHA256" +
            "\n" +
            date.full +
            "\n" +
            requestScope.datedScope(date) +
            "\n" +
            Sha256.hash(value)
}
