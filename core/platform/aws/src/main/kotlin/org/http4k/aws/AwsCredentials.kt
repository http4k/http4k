package org.http4k.aws

data class AwsCredentials(val accessKey: String, val secretKey: String, val sessionToken: String? = null) {
    override fun toString() =
        "AwsCredentials(accessKey=$accessKey, secretKey=****, sessionToken=${sessionToken?.let { "****" }})"
}
