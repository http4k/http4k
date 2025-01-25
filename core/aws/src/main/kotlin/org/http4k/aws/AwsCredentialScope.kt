package org.http4k.aws

data class AwsCredentialScope(val region: String, val service: String) {
    internal fun datedScope(date: AwsRequestDate) = "${date.basic}/$region/$service/aws4_request"
}
