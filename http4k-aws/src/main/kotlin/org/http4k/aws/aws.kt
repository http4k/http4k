package org.http4k.aws

data class AwsCredentials(val accessKey: String, val secretKey: String)

data class AwsCredentialScope(val region: String, val service: String)

internal val ALGORITHM = "AWS4-HMAC-SHA256"

internal fun AwsCredentialScope.datedScope(date: AwsRequestDate): String =
    String.format("%s/%s/%s/aws4_request",
        date.basic,
        region,
        service)