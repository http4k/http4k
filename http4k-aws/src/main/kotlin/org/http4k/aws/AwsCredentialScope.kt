package org.http4k.aws

data class AwsCredentialScope(val region: String, val service: String) {
    internal fun datedScope(date: AwsRequestDate): String =
        String.format("%s/%s/%s/aws4_request",
            date.basic,
            region,
            service)
}